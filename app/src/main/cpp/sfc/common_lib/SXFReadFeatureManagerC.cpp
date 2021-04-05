//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2000.04.23
//
//	１．クラス概要
//      Ｆｅａｔｕｒｅ Commentアセンブリ要素、
//      一般要素の親子関係を管理する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.23     周
//
//------------------------------------------------------------

#include "stdafx.h"
#include "SXFReadFeatureManagerC.h"
#include "SXFReadMapManagerC.h"
#include "SXFFeatureAssemblyC.h"
#include "SXFFeatureChildC.h"
#include "SXFSplitFeatureC.h"
#include "SXFErrorMsgC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2000.04.23
//
//  １．機能概要
//      クラス構築
//
//  ２．インターフェース
//		void	SXFReadFeatureManagerC::SXFReadFeatureManagerC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.23     周
//
//------------------------------------------------------------
SXFReadFeatureManagerC::SXFReadFeatureManagerC()
{
    //initialization
    m_SheetAssembly = NULL;
    m_CurveAssemblyList = NULL;
    m_SymbolAssemblyList = NULL;
	m_SymbolInfoTable.InitHashTable(5000);
    m_CurrentAssembly = NULL;
    m_CurrentChildList = NULL;
    m_CurrentChild=NULL;
    m_CurrentMapManager=NULL;
    m_CurrentAssemblyType=0;
    m_AssemblyPos = NULL;
    m_ChildPos = NULL;
	m_code = 1;			/* アセンブリコード */
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2000.04.23
//
//  １．機能概要
//      クラス消滅
//
//  ２．インターフェース
//		void	SXFReadFeatureManagerC::~SXFReadFeatureManagerC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.23     周
//
//------------------------------------------------------------
SXFReadFeatureManagerC::~SXFReadFeatureManagerC()
{
    if(m_SheetAssembly != NULL )
        delete m_SheetAssembly;
    if(m_CurveAssemblyList != NULL){
        m_AssemblyPos = m_CurveAssemblyList->GetHeadPosition();
        while(m_AssemblyPos!=NULL){
            m_CurrentAssembly = m_CurveAssemblyList->GetAt(m_AssemblyPos);
            if(m_CurrentAssembly!=NULL){
                delete m_CurrentAssembly;
            }
            m_CurveAssemblyList->GetNext(m_AssemblyPos);
        }
        if(m_CurveAssemblyList != NULL)
            delete  m_CurveAssemblyList;
    }
	
    if(m_SymbolAssemblyList != NULL){
        m_AssemblyPos = m_SymbolAssemblyList->GetHeadPosition();
        while(m_AssemblyPos!=NULL){
            m_CurrentAssembly = m_SymbolAssemblyList->GetAt(m_AssemblyPos);
            if(m_CurrentAssembly!=NULL){
                delete m_CurrentAssembly;
            }
            m_SymbolAssemblyList->GetNext(m_AssemblyPos);
        }
        if(m_SymbolAssemblyList != NULL)
            delete  m_SymbolAssemblyList;
    }
	/*
	CString					strKey ;
	SXFFeatureAssemblyC*	pSymValue ;
	for( POSITION pos = m_SymbolInfoTable.GetStartPosition(); pos; ) {
		m_SymbolInfoTable.GetNextAssoc(pos, strKey, (void*&)pSymValue) ;
		delete pSymValue ;
	}
	*/
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2000.04.23
//
//  １．機能概要
//      要素管理Tableに一つのFeature Comment Assembly Instanceを登録する
//      
//  ２．インターフェース
//      BOOL RegisterFeatureAssembly(INSTANCEDATA *IN_Instance)
//
//	    引数: 型			  引数名			    内容
//            INSTANCEDATA*  IN_Instance           Feature Comment Assembly
//                                                 Instance構造体のアドレス
//      
//	    復帰値	
//            正常時：TRUE
//			  異常時：FALSE
//      
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.23     周
//
//------------------------------------------------------------
BOOL SXFReadFeatureManagerC::RegisterFeatureAssembly(INSTANCEDATA *IN_Instance)
{

	/** １つ前のアセンブリオブジェクトを退避 **/
	SXFFeatureAssemblyC* LastAssembly = m_CurrentAssembly;
	
	/** ｱｾﾝﾌﾞﾘｵﾌﾞｼﾞｪｸﾄ生成 */
    m_CurrentAssembly=new SXFFeatureAssemblyC;
    if(m_CurrentAssembly == NULL){
		Delete_m_CurrentChildList();
        return FALSE;//memory error
    }
	/** インスタンスID設定 **/
    m_CurrentAssembly->AssemblyID = IN_Instance->InstanceID;
	/** アセンブリコード設定 **/
    m_CurrentAssembly->AssemblyCode = m_code;
	/** フィーチャリスト設定 **/
    m_CurrentAssembly->ChildList = m_CurrentChildList;
    m_CurrentChildList = NULL;

	/** Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
    m_CurrentMapManager =  new SXFReadMapManagerC;
    if(m_CurrentMapManager == NULL){
		delete m_CurrentAssembly;
		m_CurrentAssembly=NULL;
        return FALSE;//memory error
    }
	/** インスタンスデータ設定 **/
    if (!m_CurrentMapManager->SetFeatureData(IN_Instance)){
		delete m_CurrentAssembly;
		m_CurrentAssembly=NULL;
		delete m_CurrentMapManager;
		m_CurrentMapManager=NULL;
        return FALSE;//memory error
	}
	/** Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
    m_CurrentAssembly->AssemblyMapManager = m_CurrentMapManager;

	/** 用紙 **/
    if(IN_Instance->EntityName == "drawing_sheet_feature"){
		/* 用紙がすでに設定済み */
        if(m_SheetAssembly != NULL){
			/* 用紙は一つのみ */
			SXFErrorMsgC::RecordError(SXFSHEETONE,"SHEET",0);
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
			return FALSE;
		}
        CStringArray* array;
        SXFSplitFeatureC split(IN_Instance->EntityData);
        array = split.Split();
        if(array == NULL){
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
            return FALSE;
		}
        if (array->GetSize() != 5){ // syntax err
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
            return FALSE;
		}
		/* 用紙名 */
        m_CurrentAssembly->AssemblyName = (*array)[0];
		/* 用紙サイズ種別 */
        m_CurrentAssembly->AssemSheetType = atoi((*array)[1]);
		//用紙サイズ種別が０～４または９ではないとき
		if ( (m_CurrentAssembly->AssemSheetType < 0) 
					|| (m_CurrentAssembly->AssemSheetType > 4) ){
			if (m_CurrentAssembly->AssemSheetType != 9){
				SXFErrorMsgC::RecordError(SXFSHEETSIZE,
											"SHEET",
											1,
											m_CurrentAssembly->AssemSheetType);
				delete m_CurrentAssembly;
				m_CurrentAssembly=NULL;
				return FALSE;
			}
		}
		/* 用紙サイズ */
        m_CurrentAssembly->AssemSheetX = atoi((*array)[3]);
        m_CurrentAssembly->AssemSheetY = atoi((*array)[4]);
		//用紙サイズ種別が９で自由用紙横長,縦長が設定されてないとき
		if (m_CurrentAssembly->AssemSheetType == 9){
			if (m_CurrentAssembly->AssemSheetX <= 0){
				SXFErrorMsgC::RecordError(SXFSHEETLENGTH,
											"SHEET",
											1,
											m_CurrentAssembly->AssemSheetX);
				delete m_CurrentAssembly;
				m_CurrentAssembly=NULL;
				return FALSE;
			}
			if (m_CurrentAssembly->AssemSheetY <= 0){
				SXFErrorMsgC::RecordError(SXFSHEETLENGTH,
											"SHEET",
											1,
											m_CurrentAssembly->AssemSheetY);
				delete m_CurrentAssembly;
				m_CurrentAssembly=NULL;
				return FALSE;
			}
		}
		/* 縦／横区分 */
        m_CurrentAssembly->AssemSheetOrient = atoi((*array)[2]);
		//用紙サイズ種別が９ではなく、縦/横区分が０または１ではないとき
		//用紙サイズ種別が９のとき、縦/横区分は無効
		if (m_CurrentAssembly->AssemSheetType != 9){
			if ((m_CurrentAssembly->AssemSheetOrient < 0) 
					|| (m_CurrentAssembly->AssemSheetOrient > 1)){
				SXFErrorMsgC::RecordError(SXFORIENTERROR,
											"SHEET",
											1,
											m_CurrentAssembly->AssemSheetOrient);
				delete m_CurrentAssembly;
				m_CurrentAssembly=NULL;
				return FALSE;
			}
		}
		/* 用紙アセンブリオブジェクトセット */
        m_SheetAssembly = m_CurrentAssembly;
    }
	/** 複合曲線定義 **/
	else if(IN_Instance->EntityName == "composite_curve_org_feature"){
        /** １つ目の複合曲線の場合**/
        if(m_CurveAssemblyList == NULL){
            m_CurveAssemblyList = new SXFASSEMBLYLIST;
        }
        /** ２つ目以降の複合曲線のにて、１つ前のアセンブリ要素が用紙だった場合 **/
		if(LastAssembly==m_SheetAssembly && LastAssembly!=NULL ){
			/* 用紙は複合曲線定義に配置できない */
			SXFErrorMsgC::RecordError(SXFCURVELOCERR,"COPOSITE_CURVE_ORG",0);
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
            return FALSE;
		}
        CStringArray* array;
        SXFSplitFeatureC split(IN_Instance->EntityData);
        array = split.Split();
        if(array == NULL){
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
            return FALSE;
		}
        if (array->GetSize() != 4){ // syntax err
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
            return FALSE;
		}
		/* 属性 */
		m_CurrentAssembly->m_CurveColour = atoi((*array)[0]);
		m_CurrentAssembly->m_CurveType = atoi((*array)[1]);
		m_CurrentAssembly->m_CurveWidth = atoi((*array)[2]);
		/* 表示／非表示フラグ */
        m_CurrentAssembly->AssemCurveFlag = atoi((*array)[3]);
        //表示／非表示フラグが０または１以外のとき
        if ((m_CurrentAssembly->AssemCurveFlag < 0) 
				|| (m_CurrentAssembly->AssemCurveFlag > 1)){
            SXFErrorMsgC::RecordError(SXFCURVEFLAG,
										"COPOSITE_CURVE_ORG",
										1,
										m_CurrentAssembly->AssemCurveFlag);
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
            return FALSE;
        }
		/* 複合曲線テーブルに追加 */
        m_CurveAssemblyList->AddTail(m_CurrentAssembly);
    }
	/** 複合図形定義 **/
	else if(IN_Instance->EntityName == "sfig_org_feature"){
		/** ２つ目以降の複合図形にて、１つ前のアセンブリ要素が用紙だった場合 **/
		if(LastAssembly==m_SheetAssembly && LastAssembly!=NULL ){
			/* 用紙は複合図形定義に配置できない */
			SXFErrorMsgC::RecordError(SXFSFIGLOCERR,"SFIG_ORG",0);
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
            return FALSE;
		}
		/** １つ目の複合図形の場合 **/
		
        if(m_SymbolAssemblyList == NULL){
            m_SymbolAssemblyList = new SXFASSEMBLYLIST;
        }
		
        CStringArray* array;
        SXFSplitFeatureC split(IN_Instance->EntityData);
        array = split.Split();
        if(array == NULL){
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
            return FALSE;
		}
        if (array->GetSize() != 2){ // syntax err
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
            return FALSE;
		}
		/* 複合図形名 */
        m_CurrentAssembly->AssemblyName = (*array)[0];
		//Name Unique check

		/*	2010.08
		for(POSITION pos = m_SymbolAssemblyList->GetHeadPosition();
						pos != NULL;m_SymbolAssemblyList->GetNext(pos)){
			// 既に名前が名前が登録済み
			if (m_SymbolAssemblyList->GetAt(pos)->AssemblyName == (*array)[0]){
				SXFErrorMsgC::RecordError(SXFSAMENAME,"SFIG_ORG",1,(*array)[0]);
				delete m_CurrentAssembly;
				m_CurrentAssembly=NULL;
				return FALSE;
			}
		}
		*/

		SXFFeatureAssemblyC* pValue ;	// 2010.08
		if( m_SymbolInfoTable.Lookup(m_CurrentAssembly->AssemblyName, (void*&)pValue) ) {
			// 既に名前が名前が登録済み
			SXFErrorMsgC::RecordError(SXFSAMENAME,"SFIG_ORG",1,(*array)[0]);
			delete m_CurrentAssembly;
			m_CurrentAssembly=NULL;
			return FALSE;
		}
		/* 複合図形種別 */
		m_CurrentAssembly->AssemSymbolType = atoi((*array)[1]);
		if (m_CurrentAssembly->AssemSymbolType < 1 
				|| m_CurrentAssembly->AssemSymbolType > 4){
			CString AssemID;
			AssemID.Format("#%d",m_CurrentAssembly->AssemblyID);
			SXFErrorMsgC::RecordError(SXFSFIGFLAG,
										"SFIG_ORG",
										3,
										AssemID,
										m_CurrentAssembly->AssemSymbolType,
										"SFIG_ORG");
				delete m_CurrentAssembly;
				m_CurrentAssembly=NULL;
				return FALSE;
		}
		/* 複合図形ﾃｰﾌﾞﾙに追加 */
        m_SymbolAssemblyList->AddTail(m_CurrentAssembly);
		m_SymbolInfoTable.SetAt(m_CurrentAssembly->AssemblyName, m_CurrentAssembly);	// 2010.08
    }
    m_code++;	/* ｱｾﾝﾌﾞﾘｺｰﾄﾞ */
    return TRUE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2000.04.23
//
//  １．機能概要
//      要素管理Tableに一つのFeature Comment Entity Instanceを登録する
//      
//  ２．インターフェース
//      BOOL RegisterFeatureEntity(INSTANCEDATA *IN_Instance)
//
//	    引数: 型			  引数名			    内容
//            INSTANCEDATA*  IN_Instance           Feature Comment Entity
//                                                 Instance構造体のアドレス
//      
//	    復帰値	
//            正常時：TRUE
//			  異常時：FALSE
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.23     周
//
//------------------------------------------------------------
BOOL SXFReadFeatureManagerC::RegisterFeatureEntity(INSTANCEDATA *IN_Instance)
{
    if(m_CurrentChildList == NULL){
		/* 一般要素ﾃｰﾌﾞﾙ生成 */
        m_CurrentChildList = new SXFCHILDLIST; 
    }
	/* 一般要素ﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ生成 */
    m_CurrentChild = new SXFFeatureChildC;
    if(m_CurrentChild == NULL)
        return FALSE;//memory error
	/** インスタンスID設定 **/
    m_CurrentChild->ChildID = IN_Instance->InstanceID;
	/* ﾌｨｰﾁｬMapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
    m_CurrentMapManager =  new SXFReadMapManagerC;
    if(m_CurrentMapManager == NULL){
		delete m_CurrentChild;
		m_CurrentChild=NULL;
        return FALSE;//memory error
	}
	/** インスタンスデータ設定 **/
    if (!m_CurrentMapManager->SetFeatureData(IN_Instance)){
		delete m_CurrentChild;
		m_CurrentChild=NULL;
		delete m_CurrentMapManager;
		m_CurrentMapManager=NULL;
		return FALSE;
	}
	/* ﾌｨｰﾁｬMapping管理ｵﾌﾞｼﾞｪｸﾄ設定 */
    m_CurrentChild->ChildMapManager = m_CurrentMapManager;
	/* 一般要素ﾃｰﾌﾞﾙに追加 */
    m_CurrentChildList->AddTail( m_CurrentChild );
    return TRUE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2000.04.23
//
//  １．機能概要
//      要素管理Tableから一つの要素を出力する
//      
//  ２．インターフェース
//      int GetNextFeature(char* OUT_TypeName,
//                         LPVOID& OUT_Struct,
//                        int& OUT_EndFlg)
//
//	    引数: 型			  引数名			   内容
//            char*			  OUT_TypeName         構造体のType Name
//			  size_t		  TypeNameSize		   Type Nameバッファサイズ
//            LPVOID&         OUT_Struct           構造体のアドレス
//            int&            OUT_EndFlg           最後の要素のFlag
//                                                  1: 最後の要素
//                                                  0: 続き
//      
//	    復帰値	
//            正常時： Feature ID
//			  異常時： 負(エラーコード)
//      
//
//  ３．履歴
//		履歴番号	日付け			担当者			概要
//		作成		2000.04.23		周
//		更新		2015.04.27		T.Matsushima	JPCERT/CC 指摘 脆弱性対応
//
//------------------------------------------------------------
int SXFReadFeatureManagerC::GetNextFeature(char* OUT_TypeName, 
										   size_t TypeNameSize,	// バッファサイズを渡すように変更 JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
										   LPVOID& OUT_Struct,
										   int* OUT_EndFlg)
{
    OUT_Struct = NULL;
    *OUT_EndFlg = 1;
	int ReturnID = -1;
    m_CurrentChild=NULL;
	/** アセンブリが開かれていない **/
    if(m_CurrentAssembly == NULL){
		SXFErrorMsgC::RecordError(SXFNOTASSEMBLY,OUT_TypeName,0);
		return SXFNOTASSEMBLY;
	}
	/** アセンブリ上に要素が存在しない **/
	if(m_CurrentChildList == NULL){
		SXFErrorMsgC::RecordError(SXFNOTFEATURE,"",0);
		return SXFNOTFEATURE;
	}
	/** アセンブリ上に要素が存在しない **/
    if(m_ChildPos == NULL){
		SXFErrorMsgC::RecordError(SXFNOTFEATURE,"",0);
		return SXFNOTFEATURE;
	}
	/** 一般要素ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
    m_CurrentChild = m_CurrentChildList->GetAt(m_ChildPos);
    if(m_CurrentChild == NULL){
		/** アセンブリ上に要素が存在しない **/
		SXFErrorMsgC::RecordError(SXFNOTFEATURE,"",0);
		return SXFNOTFEATURE;
	}
	/** Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
    m_CurrentMapManager = m_CurrentChild->ChildMapManager;
    if(m_CurrentMapManager == NULL){
		/* 内部エラーが発生しました */
		SXFErrorMsgC::RecordError(SXFERROR,"",0);
		return SXFERROR;
	}
	/** インスタンスID設定 **/
	ReturnID = m_CurrentChild->ChildID;
	/** フィーチャ構造体取得 **/
	//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
	//OUT_Struct = m_CurrentMapManager->GetStructData( OUT_TypeName);
	OUT_Struct = m_CurrentMapManager->GetStructData( OUT_TypeName, TypeNameSize);
	if(OUT_Struct == NULL){
		/* 不正なエンティティ名です */
 		SXFErrorMsgC::RecordError(SXFERRENTITY,"",0);
		return SXFERRENTITY;
	}
	m_CurrentChildList->GetNext(m_ChildPos);
	/** 構造体返却済みフラグをON **/
	m_CurrentMapManager->SetReturnFlag();
	/** 一般要素ﾃｰﾌﾞﾙの最後の要素のとき **/
    if(m_ChildPos == NULL){
        *OUT_EndFlg = 1;		/* EndFlg をON */
    }
	else{
        *OUT_EndFlg = 0;		/* EndFlg をOFF */
    }
    return ReturnID;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2000.04.23
//
//  １．機能概要
//      Assembly要素管理Tableから一つのAssembly要素を出力する
//      
//  ２．インターフェース
//      int GetNextAssembly(int IN_Type,
//                          LPVOID& OUT_Struct,
//                          int& OUT_EndFlg,
//                          int& OUT_InstanceID)
//
//	    引数: 型			  引数名			    内容
//            int             IN_Type              Assembly構造体のType
//                                                  1:sheet
//                                                  2:symbol
//                                                  3:curve
//            LPVOID&         OUT_Struct           Assembly構造体のアドレス
//            int&            OUT_EndFlg           最後のAssembly要素のFlag
//                                                  1: 最後のAssembly要素
//                                                  0: 続き
//            int&            OUT_InstanceID        Assembly Feature ID
//      
//	    復帰値	
//            正常時：Assembly Code(Assembly Tableに登録したの番号）
//			  異常時：負(エラーコード)
//      
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.23     周
//
//------------------------------------------------------------
int SXFReadFeatureManagerC::GetNextAssembly(int IN_Type, 
												LPVOID& OUT_Struct, 
												int* OUT_EndFlg,
												int& OUT_InstanceID)
{
    OUT_Struct = NULL;
    *OUT_EndFlg = 1;
	OUT_InstanceID = -1;
    m_CurrentAssembly = NULL;
    m_CurrentChildList = NULL;    
    m_ChildPos = NULL;

	/* ｱｾﾝﾌﾞﾘ型が異常 */
	if (IN_Type != 1 && IN_Type != 2 && IN_Type != 3){
		SXFErrorMsgC::RecordError(SXFASSEMTYPEERR,"",1,IN_Type);
		return SXFASSEMTYPEERR;
	}
	/* 新規ﾃｰﾌﾞﾙの読み出し指定 */
	if(m_CurrentAssemblyType != IN_Type){
		m_CurrentAssemblyType = 0;
	}
	/* 新規ﾃｰﾌﾞﾙの読み出し指定 */
	if(m_CurrentAssemblyType != IN_Type){
		m_CurrentAssemblyType = 0;
	}

	/** 最初のアセンブリ要素のとき **/
    if(m_CurrentAssemblyType == 0){
		/** 用紙 **/
        if(IN_Type == 1){
			/** 用紙がない **/
            if(m_SheetAssembly == NULL){
				SXFErrorMsgC::RecordError(SXFNOTSETSHEET,"SHEET",0);
                return SXFNOTSETSHEET;//no sheet assembly
			}
            m_CurrentAssembly = m_SheetAssembly;
            m_AssemblyPos = NULL;
			/* 現在のｱｾﾝﾌﾞﾘ型をセット */
            m_CurrentAssemblyType = 1;
        }
		/** 複合図形定義 **/
		else if(IN_Type == 2){//symbol

            if(m_SymbolAssemblyList == NULL){
				// 読み出すアセンブリ要素がありません
// MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOASSEMBLY,"SFIGORG",0);
				SXFErrorMsgC::RecordError2(SXFNOASSEMBLY,"SFIGORG",0);
				return SXFNOASSEMBLY;
// MOD(E)	メッセージ修正	K.Naono	04.03.05
			}

			/** 複合図形リストの先頭アドレス設定 **/
			m_AssemblyPos=m_SymbolAssemblyList->GetHeadPosition();
            // m_AssemblyPos=m_SymbolInfoTable.GetStartPosition(); // 2010.08
            if(m_AssemblyPos == NULL){
				/* 読み出すアセンブリ要素がありません。*/
// MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOASSEMBLY,"SFIGORG",0);
				SXFErrorMsgC::RecordError2(SXFNOASSEMBLY,"SFIGORG",0);
				return SXFNOASSEMBLY;
// MOD(E)	メッセージ修正	K.Naono	04.03.05
			}
            m_CurrentAssembly = m_SymbolAssemblyList->GetAt(m_AssemblyPos);
			// CString	strKey ;									// 2010.08
			// m_SymbolInfoTable.GetNextAssoc(m_AssemblyPos, strKey, (void*&)m_CurrentAssembly);
			/* 現在のｱｾﾝﾌﾞﾘ型をセット */
            m_CurrentAssemblyType = 2;
        }
		/** 複合曲線定義 **/
		else if(IN_Type == 3){//curve
            if(m_CurveAssemblyList == NULL){
				/* 読み出すアセンブリ要素がありません。*/
// MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOASSEMBLY,"CCURVE_ORG",0);
				SXFErrorMsgC::RecordError2(SXFNOASSEMBLY,"CCURVE_ORG",0);
				return SXFNOASSEMBLY;
// MOD(E)	メッセージ修正	K.Naono	04.03.05
			}
			/** 複合曲線リストの先頭アドレス設定 **/
            m_AssemblyPos=m_CurveAssemblyList->GetHeadPosition();
            if(m_AssemblyPos == NULL){
				/* 読み出すアセンブリ要素がありません。*/
// MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOASSEMBLY,"CCURVE_ORG",0);
				SXFErrorMsgC::RecordError2(SXFNOASSEMBLY,"CCURVE_ORG",0);
				return SXFNOASSEMBLY;
// MOD(E)	メッセージ修正	K.Naono	04.03.05
			}
            m_CurrentAssembly = m_CurveAssemblyList->GetAt(m_AssemblyPos);
			/* 現在のｱｾﾝﾌﾞﾘ型をセット */
            m_CurrentAssemblyType = 3;
        }
    }
	/** ２つ目以降のアセンブリ要素のとき **/
	else{
		/* 最終データ取得後の読み出しチェック */
		if (m_AssemblyPos == NULL){
			/* 読み出すアセンブリ要素がありません。*/
			SXFErrorMsgC::RecordError(SXFNOASSEMBLY,"",0);
			return SXFNOASSEMBLY;
		}
		/* 複合図形 */
		if(m_CurrentAssemblyType == 2) {
		    m_CurrentAssembly = m_SymbolAssemblyList->GetAt(m_AssemblyPos);
			// CString	strKey ;									// 2010.08
			// m_SymbolInfoTable.GetNextAssoc(m_AssemblyPos, strKey, (void*&)m_CurrentAssembly);
		}
		/* 複合曲線 */
        else if(m_CurrentAssemblyType == 3)
            m_CurrentAssembly = m_CurveAssemblyList->GetAt(m_AssemblyPos);
    }
    if(m_CurrentAssembly == NULL){
		/* 読み出すアセンブリ要素がありません。*/
		SXFErrorMsgC::RecordError(SXFNOASSEMBLY,"",0);
		return SXFNOASSEMBLY;
	}
	/** Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ **/
    m_CurrentMapManager = m_CurrentAssembly->AssemblyMapManager;
    if(m_CurrentMapManager == NULL){
		/* 内部エラーが発生しました */
		SXFErrorMsgC::RecordError(SXFERROR,"",0);
		return SXFERROR;
	}
	/** アセンブリフィーチャの構造体取得 **/
    OUT_Struct = m_CurrentMapManager->GetAssemblyStructData( );
	/** インスタンスID設定 **/
	OUT_InstanceID = m_CurrentAssembly->AssemblyID ;//エラーメッセージに必要！
	/** フィーチャリスト設定 **/
    m_CurrentChildList=m_CurrentAssembly->ChildList;
	if(m_CurrentChildList == NULL){
		/** エラーフラグをたてる **/
		m_CurrentAssembly->m_AssemblyErrFlag=FALSE;
	}
	else{
		/** フィーチャリストの先頭アドレスを設定 **/
		m_ChildPos = m_CurrentChildList->GetHeadPosition();
		    if(m_ChildPos == NULL){
				/** エラーフラグをたてる **/
				m_CurrentAssembly->m_AssemblyErrFlag=FALSE;
		    }
	}

	/** 用紙 **/
    if(IN_Type == 1)
        m_AssemblyPos = NULL;
	/** 複合図形 **/
	else if(IN_Type == 2) {
        m_SymbolAssemblyList->GetNext(m_AssemblyPos);
		// CString	strKey ;
		// SXFFeatureAssemblyC* pSymValue ;
		// m_SymbolInfoTable.GetNextAssoc(m_AssemblyPos, strKey, (void*&)pSymValue);
	}
    /** 複合曲線 **/
	else if(IN_Type == 3)
        m_CurveAssemblyList->GetNext(m_AssemblyPos);
	/** 構造体返却済みフラグをON **/
	m_CurrentMapManager->SetReturnFlag();
	/** アセンブリテーブルの最後の要素のとき **/
    if(m_AssemblyPos == NULL){
        *OUT_EndFlg = 1;	/** END flg をON **/
    }
	else{
        *OUT_EndFlg = 0;	/** END flg をOFF **/
    }
    return m_CurrentAssembly->AssemblyCode;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2001.2.23
//
//  １．機能概要
//      複合図形名に対応する複合図形種別の取得
//
//  ２．インターフェース
//      BOOL GetSfigFlg( LPCTSTR IN_Name,
//						int* OUT_SfigFlg,
//						int* OUT_LocFlg)
//
//	    引数: 型			  引数名			内容
//            LPCTSTR         IN_Name           Sfig Name
//			  int*			  OUT_SfigFlg		複合図形種別
//			  int*			  OUT_LocFlg		配置フラグ
//												(1:配置済 0:未配置)
//
//	    復帰値	
//            正常時：TRUE		複合図形名に対応する種別あり
//			  異常時：FALSE		複合図形名に対応する種別なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者		 概要
//      作成		2001.2.23      宮園(FQS)
//
//------------------------------------------------------------
BOOL SXFReadFeatureManagerC::GetSfigFlg( LPCTSTR IN_Name,
											int* OUT_SfigFlg,
											int* OUT_LocFlg)
{
	*OUT_SfigFlg = -1;
	*OUT_LocFlg = -1;
/*
	if (m_SymbolAssemblyList == NULL)
		return FALSE;
	//Name check 
	for(POSITION pos = m_SymbolAssemblyList->GetHeadPosition();
					pos != NULL;m_SymbolAssemblyList->GetNext(pos)){
		if (m_SymbolAssemblyList->GetAt(pos)->AssemblyName == IN_Name){
			// 複合図形種別
			*OUT_SfigFlg = m_SymbolAssemblyList->GetAt(pos)->AssemSymbolType;
			// 複合図形配置ﾌﾗｸﾞ
			*OUT_LocFlg = m_SymbolAssemblyList->GetAt(pos)->m_SfigLocateFlag;
			return TRUE;
        }
	}
*/
	SXFFeatureAssemblyC* pValue ;
	if( m_SymbolInfoTable.Lookup(IN_Name, (void*&)pValue) ) {
		// 複合図形種別
		*OUT_SfigFlg= pValue->AssemSymbolType ;
		// 複合図形配置ﾌﾗｸﾞ
		*OUT_LocFlg	= pValue->m_SfigLocateFlag ;
		return TRUE ;
	}
	return FALSE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2001.2.23
//
//  １．機能概要
//      複合図形名に対応する複合図形種別の設定
//
//  ２．インターフェース
//      BOOL SetLocFlg( LPCTSTR IN_Name)
//
//	    引数: 型			  引数名			内容
//            LPCTSTR         IN_Name           Sfig Name
//
//	    復帰値	
//            正常時：TRUE		複合図形名に対応する種別あり
//			  異常時：FALSE		複合図形名に対応する種別なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2001.2.23	   宮園(FQS)
//
//------------------------------------------------------------
BOOL SXFReadFeatureManagerC::SetLocFlg( LPCTSTR IN_Name)
{
/*
	if (m_SymbolAssemblyList == NULL)
		return FALSE;
	for(POSITION pos = m_SymbolAssemblyList->GetHeadPosition();
					pos != NULL;m_SymbolAssemblyList->GetNext(pos)){
		if (m_SymbolAssemblyList->GetAt(pos)->AssemblyName == IN_Name){
			// 複合図形配置ﾌﾗｸﾞをON 
			m_SymbolAssemblyList->GetAt(pos)->m_SfigLocateFlag = 1;
			return TRUE;
        }
	}
*/

/*
	for(int i=0;i<m_SymbolInfoTable.GetSize();i++){
		if (m_SymbolInfoTable[i].name == IN_Name){
			// 複合図形配置ﾌﾗｸﾞをON
			m_SymbolInfoTable[i].locateflag = 1;
			return TRUE;
		}
	}
*/
	SXFFeatureAssemblyC* pValue ;
	if( m_SymbolInfoTable.Lookup(IN_Name, (void*&)pValue) ) {
		// 複合図形配置ﾌﾗｸﾞをON
		pValue->m_SfigLocateFlag = 1 ;
		return TRUE ;
	}
	return FALSE;  
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2001.2.23
//
//  １．機能概要
//      ハッチングの外形・中抜きIDが定義されているかチェックする
//
//  ２．インターフェース
//      BOOL CheckHatchID( int IN_ID)
//
//	    引数: 型	引数名	    内容
//            int   IN_ID       ﾊｯﾁﾝｸﾞの外形中抜きのID
//								(複合曲線のｱｾﾝﾌﾞﾘｺｰﾄﾞ)
//
//	    復帰値	
//            正常時：TRUE		ﾊｯﾁﾝｸﾞの外形が定義されている
//			  異常時：FALSE		ﾊｯﾁﾝｸﾞの外形が定義されていない
//      
//  ３．履歴
//	    履歴番号    日付け         担当者		概要
//      作成		2001.2.23	   宮園(FQS)
//
//------------------------------------------------------------
BOOL SXFReadFeatureManagerC::CheckHatchID( int IN_ID){
	if (m_CurveAssemblyList == NULL)
		return FALSE;
	for(POSITION pos = m_CurveAssemblyList->GetHeadPosition();
				pos != NULL;m_CurveAssemblyList->GetNext(pos)){
		if (m_CurveAssemblyList->GetAt(pos)->AssemblyCode == IN_ID)
			// 無条件にTRUEにするのではなく、errorであるかどうかも調べる
			if (m_CurveAssemblyList->GetAt(pos)->m_AssemblyErrFlag == TRUE) {
				return TRUE;
			}
			else {
				return FALSE;
			}
	}
	return FALSE;			
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.cpp               作成日：2001.2.23
//
//  １．機能概要
//      m_CurrentChildListをdeleteする
//
//  ２．インターフェース
//      void Delete_m_CurrentChildList()
//
//	    引数: 型	引数名	    内容
//
//	    復帰値	
//      
//  ３．履歴
//	    履歴番号    日付け         担当者		概要
//      作成		2001.2.23	   宮園(FQS)
//
//------------------------------------------------------------
void SXFReadFeatureManagerC::Delete_m_CurrentChildList(){
	if(m_CurrentChildList != NULL){
		POSITION childpos = m_CurrentChildList->GetHeadPosition();
		while(childpos!=NULL){
			SXFFeatureChildC* child = m_CurrentChildList->GetAt(childpos);
			if(child!=NULL)
				delete child;
			m_CurrentChildList->GetNext(childpos);
		}
		delete m_CurrentChildList;
		m_CurrentChildList = NULL;
	}
}


