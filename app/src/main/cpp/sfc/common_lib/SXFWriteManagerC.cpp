//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2000.04.18
//
//	１．クラス概要
//      出力時のアセンブリ間の参照関係を管理する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18    周
//
//------------------------------------------------------------

#include "stdafx.h"
#include "SXFWriteManagerC.h"
#include "SXFErrorMsgC.h"
#include "SXFPart21FileWriteC.h"
#include "SXFInstanceIDC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2000.04.18
//
//  １．機能概要
//      クラス構築
//
//  ２．インターフェース
//		void	SXFWriteManagerC::SXFWriteManagerC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18    周
//
//------------------------------------------------------------
SXFWriteManagerC::SXFWriteManagerC()
{
    //initialization
    m_ChildrenArray = NULL;
    m_CurrentAssembly = NULL;
    m_SheetAssemblyFlag=FALSE;
	m_CurrentChild = NULL;
    m_FirstAssemblyFlag=TRUE;
	m_AssemType = -1;
	m_SfigFlag = -1;
	m_SfigTableIndex = 0;
	m_AssemblyCode = 1;
	m_CurveFlag = 0;
	m_ErrorFlag = FALSE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2000.04.18
//
//  １．機能概要
//      クラス構築
//
//  ２．インターフェース
//		void	SXFWriteManagerC::SXFWriteManagerC(
//								SXFPart21FileWriteC* IN_WriteFile,
//								SXFInstanceIDC* IN_InstanceIDManager)
//
//		
//	引数：型					引数名					内容
//		  SXFPart21FileWriteC*	IN_WriteFile			ﾌｧｲﾙ出力ｵﾌﾞｼﾞｪｸﾄ
//		  SXFInstanceIDC*		IN_InstanceIDManager	ｲﾝｽﾀﾝｽIDｵﾌﾞｼﾞｪｸﾄ
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18    周
//
//------------------------------------------------------------
SXFWriteManagerC::SXFWriteManagerC(SXFPart21FileWriteC* IN_WriteFile,
									SXFInstanceIDC* IN_InstanceIDManager)
{
    //initialization
    m_ChildrenArray = NULL;
    m_CurrentAssembly = NULL;
	m_SheetAssemblyFlag=FALSE;
	m_CurrentChild = NULL;
    m_FirstAssemblyFlag=TRUE;
	m_WriteFile = IN_WriteFile;
	m_InstanceIDManager = IN_InstanceIDManager;
	m_AssemType = -1;
	m_SfigFlag = -1;
	m_SfigTableIndex = 0;
	m_AssemblyCode = 1;
	m_CurveFlag = 0;
	m_ErrorFlag = FALSE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2000.04.18
//
//  １．機能概要
//      クラス消滅
//
//  ２．インターフェース
//		void	SXFWriteManagerC::~SXFWriteManagerC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18    周
//
//------------------------------------------------------------
SXFWriteManagerC::~SXFWriteManagerC()
{
	if (m_CurrentAssembly != NULL)
		delete m_CurrentAssembly;
	if (m_ChildrenArray != NULL)
		delete m_ChildrenArray;
	if (m_CurrentChild != NULL)
		delete m_CurrentChild;
	for( POSITION pos = m_SfigorgTable.GetHeadPosition(); pos; ) {
		SXFSFIGORGCHECK*	pSfigOrg = m_SfigorgTable.GetNext(pos) ;
		delete pSfigOrg ;
	}
}


//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2000.04.18
//
//  １．機能概要
//      Assembly要素の登録
//
//  ２．インターフェース
//      int RegisterAssembly(int IN_AssemblyType,LPVOID IN_Struct)
//
//	    引数: 型			  引数名			    内容
//            int             IN_AssemblyType      Assembly Type
//            LPVOID          IN_Struct            Assembly要素の
//                                                 構造体のアドレス
//      
//	    復帰値	
//            正常時：Assembly要素のCode
//			  異常時：負(エラーコード)
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18    周
//
//------------------------------------------------------------
int SXFWriteManagerC::RegisterAssembly(int IN_AssemblyType,LPVOID IN_Struct)
{
	int error_code;

	/** すでに用紙がある場合 **/
    if( m_SheetAssemblyFlag == TRUE){
		m_AssemType = -1;	/* アセンブリは開かれていない */
		/* 用紙 */
		if (IN_AssemblyType == 1){
			/* 用紙は１つしか設定できない */
			SXFErrorMsgC::RecordError(SXFSHEETONE,
										"SHEET",
										0);
			return SXFSHEETONE;
		}
		else if (IN_AssemblyType == 2){
			/* 複合図形上に用紙は配置できない */
			SXFErrorMsgC::RecordError(SXFSFIGLOCERR,
										"SFIG_ORG",
										0);
			return SXFSFIGLOCERR;
		}
		else if (IN_AssemblyType == 3){
			/* 複合曲線上には用紙は配置できない */
			SXFErrorMsgC::RecordError(SXFCURVELOCERR,
										"COMPOSITE_CURVE_ORG",
										0);
			return SXFCURVELOCERR;
		}
    }
	/** 用紙 **/
    if(IN_AssemblyType == 1)
        m_SheetAssemblyFlag=TRUE;
	/** 複合図形 **/
    else if(IN_AssemblyType == 2){
		/*
        SXFSFIGORGCHECK sfigorgcheck;
        sfigorgcheck.name = ((Sfigorg_Struct*)IN_Struct)->name;
        sfigorgcheck.type = ((Sfigorg_Struct*)IN_Struct)->flag;
		sfigorgcheck.locate_flag = -1;
		sfigorgcheck.error = -1;
		// 複合図形ﾃｰﾌﾞﾙに追加
        m_SfigorgTable.Add(sfigorgcheck );
		*/
		SXFSFIGORGCHECK*	pSfigOrg = new SXFSFIGORGCHECK ;
		pSfigOrg->name = ((Sfigorg_Struct*)IN_Struct)->name ;
		pSfigOrg->type = ((Sfigorg_Struct*)IN_Struct)->flag ;
		m_SfigorgTable.AddTail(pSfigOrg) ;
		m_mapSfigOrg.SetAt(pSfigOrg->name, pSfigOrg) ;
    }
	//AssemblyType set
	m_AssemType = IN_AssemblyType;
	/** 最初のアセンブリ要素の場合 **/
    if(m_FirstAssemblyFlag){ //first assembly
		/** WriteMapping管理オブジェクト生成 **/
		m_CurrentAssembly = new SXFWriteMappingManagerC(m_AssemType,
														m_WriteFile,
														m_InstanceIDManager);
		/** FirstAssemblyFlag をOFF **/
        m_FirstAssemblyFlag=!m_FirstAssemblyFlag;
		/** ChildrenArrayを生成 **/
		m_ChildrenArray = new CUIntArray();
    }
	else{ //not the first assembly
		//assembly must have children
		if (m_ChildrenArray->GetSize() <= 0){
			/* 前回ｾｯﾄしたｱｾﾝﾌﾞﾘ要素がｴﾗｰ */
			if (m_ErrorFlag)
				m_ErrorFlag = FALSE;
			else{
				/** 複合図形定義がエラー **/
				if (m_SfigFlag == 1){
					/* 複合図形ﾃｰﾌﾞﾙのｴﾗｰﾌﾗｸﾞをON */
					// m_SfigorgTable[m_SfigTableIndex].error = -1;	/* error flgをON */
					POSITION	pos = m_SfigorgTable.FindIndex(m_SfigTableIndex) ;
					if( pos != NULL ) {
						SXFSFIGORGCHECK*	pSfigOrg = m_SfigorgTable.GetAt(pos) ;
						pSfigOrg->error = -1 ;
					}
					m_SfigTableIndex++;
				}
				/* 複合曲線がエラー */
				if (m_CurveFlag == 1)
					/* 複合曲線テーブルからエラーのアセンブリコードを削除 */
					m_CurveTable.RemoveAt(m_CurveTable.GetSize()-1);
				/* アセンブリ要素上に要素がない */
				SXFErrorMsgC::RecordError(SXFNOTFEATURE,"",0);
			}
		}
		else{
			/** アセンブリ要素のMapping **/
			error_code = m_CurrentAssembly->MappingAssembly();
			/* Mappingに失敗 */
			if (error_code){
				/** 複合図形定義 **/
				if (m_SfigFlag == 1){
					/* 複合図形ﾃｰﾌﾞﾙのｴﾗｰﾌﾗｸﾞをON */
					// m_SfigorgTable[m_SfigTableIndex].error = -1;
					POSITION	pos = m_SfigorgTable.FindIndex(m_SfigTableIndex) ;
					if( pos != NULL ) {
						SXFSFIGORGCHECK*	pSfigOrg = m_SfigorgTable.GetAt(pos) ;
						pSfigOrg->error = -1 ;
					}
					m_SfigTableIndex++;
				}
				/* 複合曲線 */
				if (m_CurveFlag == 1)
					/* 複合曲線テーブルからエラーのアセンブリコードを削除 */
					m_CurveTable.RemoveAt(m_CurveTable.GetSize()-1);
			}
			/* Mapping成功 */
			else{
				if (m_SfigFlag == 1){
					/* 複合図形ﾃｰﾌﾞﾙのｴﾗｰﾌﾗｸﾞをOFF */
					// m_SfigorgTable[m_SfigTableIndex].error = 1;
					POSITION	pos = m_SfigorgTable.FindIndex(m_SfigTableIndex) ;
					if( pos != NULL ) {
						SXFSFIGORGCHECK*	pSfigOrg = m_SfigorgTable.GetAt(pos) ;
						pSfigOrg->error = 1 ;
					}
					m_SfigTableIndex++;
				}
				/** アセンブリコードを＋１ **/
				m_AssemblyCode++;
			}
		}
		/* データのクリア */
		m_ChildrenArray->RemoveAll();	
		m_CurrentAssembly->ClearData();
    }
	/** 複合曲線定義 **/
	if(IN_AssemblyType == 3){
		/* 複合曲線ﾌﾗｸﾞON */
		m_CurveFlag = 1;
		/* 複合曲線テーブルにアセンブリコード追加 */
		m_CurveTable.Add(m_AssemblyCode);
	}
	else{
		/* 複合曲線ﾌﾗｸﾞOFF */
		m_CurveFlag = 0;	
	}
	/** 複合図形 **/
	if (IN_AssemblyType == 2)
		m_SfigFlag = 1;		/* SfigFlag をON */
	else
		m_SfigFlag = 0;		/* SfigFlag をOFF */
	/** アセンブリ構造体をセット **/
	m_CurrentAssembly->SetStructData(IN_AssemblyType,IN_Struct);
    return m_AssemblyCode;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2000.04.18
//
//  １．機能概要
//      Feature要素構造体の登録
//
//  ２．インターフェース
//      int RegisterFeature(char* IN_FeatureType,LPVOID IN_Struct)
//
//	    引数: 型			  引数名			    内容
//            char*          IN_FeatureType        Feature要素の
//                                                 TypeName
//            LPVOID          IN_Struct            Feature要素の
//                                                 構造体のアドレス
//      
//	    復帰値	
//            正常時：要素のID
//			  異常時：負(エラーコード)
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18    周
//
//------------------------------------------------------------
int SXFWriteManagerC::RegisterFeature(char* IN_FeatureType,LPVOID IN_Struct)
{
	int error_code,childid1;

	/* アセンブリ要素が開かれていない */
	if (m_AssemType == -1){
		SXFErrorMsgC::RecordError(SXFNOTASSEMBLY,IN_FeatureType,0);
		return SXFNOTASSEMBLY;
	}
	/* Mapping管理ｵﾌﾞｼﾞｪｸﾄを生成 */
	m_CurrentChild = new SXFWriteMappingManagerC(m_AssemType,
												m_WriteFile,
												m_InstanceIDManager);
	/* フィーチャ構造体をセット&出力 */
    error_code = m_CurrentChild->MappingFeature(IN_FeatureType,IN_Struct,childid1);
	if (!error_code)
	    m_ChildrenArray->Add(childid1);
	else
		childid1 = error_code;
	delete	m_CurrentChild;
	m_CurrentChild = NULL;
    return childid1;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2000.04.18
//
//  １．機能概要
//      Sheet Assembly 要素の出力
//
//  ２．インターフェース
//      int CloseAssembly()
//
//	    引数: 型			  引数名			    内容
//			  なし
//	
//	    復帰値	
//            正常時：0
//			  異常時：負(エラーコード)
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18     周
//
//------------------------------------------------------------
int SXFWriteManagerC::CloseAssembly()
{
	int error_code;
	/* 用紙が設定されていない */
    if( m_SheetAssemblyFlag != TRUE){
		SXFErrorMsgC::RecordError(SXFNOTSETSHEET,"SHEET",0);
        return SXFNOTSETSHEET;//no sheet assembly
    }
	//assembly have not child error
	if (m_ChildrenArray->GetSize() <= 0){
		SXFErrorMsgC::RecordError(SXFNOTFEATURE,"",0);
		return SXFNOTFEATURE;
	}
	/* アセンブリ要素のMapping */
	error_code = m_CurrentAssembly->MappingAssembly( );
    return error_code;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2000.04.18
//
//  １．機能概要
//      Assembly Typeの取得
//
//  ２．インターフェース
//      int GetAssemType()
//
//	    引数: 型			  引数名			    内容
//			  なし
//
//	    復帰値	
//            正常時：Assembly Type
//			  異常時：
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18    周
//
//------------------------------------------------------------
int SXFWriteManagerC::GetAssemType()
{
	return m_AssemType;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2000.04.18
//
//  １．機能概要
//      複合図形名がすでに定義されているかチェックする
//
//  ２．インターフェース
//      BOOL SfigNameCheck( LPCTSTR IN_Name)
//
//	    引数: 型			  引数名			    内容
//            LPCTSTR         IN_Name              Sfig Name
//
//	    復帰値	
//            正常時：TRUE		定義済み
//			  異常時：FALSE		定義されていない
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18    周
//
//------------------------------------------------------------
BOOL SXFWriteManagerC::SfigNameCheck( LPCTSTR IN_Name)
{
/*
	for(int i=0;i<m_SfigorgTable.GetSize();i++){
		if ( m_SfigorgTable[i].name == IN_Name 
			&& m_SfigorgTable[i].error == 1){ 
			return TRUE;
        }
	}
*/
	SXFSFIGORGCHECK*	pSfigOrg = NULL ;
	if( m_mapSfigOrg.Lookup(IN_Name, (void*&)pSfigOrg) ) {
		if( pSfigOrg->error == 1 ) {
			return TRUE ;
		}
	}

	return FALSE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2000.04.18
//
//  １．機能概要
//      アセンブリ要素の構造体がエラーだった場合の処理
//      ＳｅｔされているアセンブリをMappingする。
//
//  ２．インターフェース
//      void ErrorAssembly()
//
//	    引数: 型			  引数名			    内容
//			  なし
//
//	    復帰値:	
//            なし
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18     周
//
//------------------------------------------------------------
void SXFWriteManagerC::ErrorAssembly()
{
	int error_code;

	//アセンブリは開かない
	m_AssemType = -1;
	//assembly must have children
    if ( m_ChildrenArray !=NULL && m_ChildrenArray->GetSize() > 0){
		/* Mapping */
		error_code = m_CurrentAssembly->MappingAssembly( );	/* ｴﾗｰｺｰﾄﾞは返さない */
		/* Mappingに失敗 */
		if (error_code){
			/** 複合図形定義 **/
			if (m_SfigFlag == 1){
				/* 複合図形ﾃｰﾌﾞﾙのｴﾗｰﾌﾗｸﾞをON */
				// m_SfigorgTable[m_SfigTableIndex].error = -1;
				POSITION	pos = m_SfigorgTable.FindIndex(m_SfigTableIndex) ;
				if( pos != NULL ) {
					SXFSFIGORGCHECK*	pSfigOrg = m_SfigorgTable.GetAt(pos) ;
					pSfigOrg->error = -1 ;
				}
				m_SfigTableIndex++;
			}
			/* 複合曲線 */
			if (m_CurveFlag == 1)
				/* 複合曲線テーブルからエラーのアセンブリコードを削除 */
				m_CurveTable.RemoveAt(m_CurveTable.GetSize()-1);
		}
		else{
			/** 複合図形定義 **/
			if (m_SfigFlag == 1){
				/* 複合図形ﾃｰﾌﾞﾙのｴﾗｰﾌﾗｸﾞをOFF */
				// m_SfigorgTable[m_SfigTableIndex].error = 1;
				POSITION	pos = m_SfigorgTable.FindIndex(m_SfigTableIndex) ;
				if( pos != NULL ) {
					SXFSFIGORGCHECK*	pSfigOrg = m_SfigorgTable.GetAt(pos) ;
					pSfigOrg->error = 1 ;
				}
				m_SfigTableIndex++;
			}
			/* アセンブリコード＋１ */
			m_AssemblyCode++;
		}
		/* 一般要素テーブルをクリア */
		m_ChildrenArray->RemoveAll();	
	}
	else{
		if (!m_FirstAssemblyFlag && !m_ErrorFlag)
			/* アセンブリ要素上に要素がない */
			SXFErrorMsgC::RecordError(SXFNOTFEATURE,"",0);
	}
	/* アセンブリデータをクリア */
	if(m_CurrentAssembly != NULL)
        m_CurrentAssembly->ClearData();
	/* ErrorFlagをON */
	m_ErrorFlag = TRUE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2001.2.23
//
//  １．機能概要
//      複合図形名に対応する複合図形種別を取得する
//
//  ２．インターフェース
//      BOOL GetSfigFlg( LPCTSTR IN_Name,
//							int* OUT_Type,
//							int* OUT_LocFlg)
//
//	    引数: 型			  引数名	    内容
//            LPCTSTR         IN_Name       Sfig Name
//			  int*			  OUT_Type		複合図形種別
//			  int*			  OUT_LocFlg	配置フラグ
//											(0：未配置 1:配置済)
//
//	    復帰値	
//            正常時：TRUE		対応する種別あり
//			  異常時：FALSE		対応する種別なし
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2001.2.23	   宮園(FQS)
//
//------------------------------------------------------------
BOOL SXFWriteManagerC::GetSfigFlg( LPCTSTR IN_Name,
									int* OUT_Type,
									int* OUT_LocFlg)
{
	*OUT_Type = -1;
	*OUT_LocFlg = -1;
/*
	for(int i=0;i<m_SfigorgTable.GetSize();i++){
		if ( m_SfigorgTable[i].name == IN_Name 
					&& m_SfigorgTable[i].error == 1){ 
			*OUT_Type = m_SfigorgTable[i].type;
			*OUT_LocFlg = m_SfigorgTable[i].locate_flag;
			return TRUE;
        }
	}
*/
	SXFSFIGORGCHECK*	pSfigOrg = NULL ;
	if( m_mapSfigOrg.Lookup(IN_Name, (void*&)pSfigOrg) ) {
		if( pSfigOrg->error == 1 ) {
			*OUT_Type	= pSfigOrg->type ;
			*OUT_LocFlg	= pSfigOrg->locate_flag ;
			return TRUE ;
		}
	}
	return FALSE;  
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2001.2.23
//
//  １．機能概要
//      複合図形名に対応する複合図形種別を設定する
//
//  ２．インターフェース
//      BOOL SetLocFlg( LPCTSTR IN_Name)
//
//	    引数: 型			  引数名	    内容
//            LPCTSTR         IN_Name       Sfig Name
//
//	    復帰値	
//            正常時：TRUE		対応するフラグあり
//			  異常時：FALSE		対応するフラグなし
//      
//  ３．履歴
//	    履歴番号    日付け         担当者		概要
//      作成		2001.2.23		宮園(FQS)
//
//------------------------------------------------------------
BOOL SXFWriteManagerC::SetLocFlg( LPCTSTR IN_Name)
{
/*
	for(int i=0;i<m_SfigorgTable.GetSize();i++){
		if ( m_SfigorgTable[i].name == IN_Name 
			&& m_SfigorgTable[i].error == 1){ 
			m_SfigorgTable[i].locate_flag = 1;
			return TRUE;
        }
	}    
*/
	SXFSFIGORGCHECK*	pSfigOrg = NULL ;
	if( m_mapSfigOrg.Lookup(IN_Name, (void*&)pSfigOrg) ) {
		if( pSfigOrg->error == 1 ) {
			pSfigOrg->locate_flag = 1 ;
			return TRUE ;
		}
	}
	return FALSE;  
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.cpp                    作成日：2001.2.23
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
//      作成		2001.2.23		宮園(FQS)
//
//------------------------------------------------------------
BOOL SXFWriteManagerC::CheckHatchID( unsigned int IN_ID){
	for (int i = 0; i < m_CurveTable.GetSize(); i++){
		if (m_CurveTable[i] == IN_ID)
			return TRUE;
	}
	return FALSE;
}
