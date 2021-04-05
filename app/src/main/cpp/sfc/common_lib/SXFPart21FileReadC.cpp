//---------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//	１．クラス概要
//      ファイルの入力を管理する
//
//	２．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//		更新		2002.02.20	K.Naono		ヘッダ解析処理の改善対応
//
//---------------------------------------------------------

#include "stdafx.h"
#include "SXFDataStructC.h"
//	MOD(S)	ヘッダ解析処理の改善対応	K.Naono		02.02.20
//#include "SXFPart21FileC.h"
#include "SXFPart21FileExC.h"
//	MOD(E)	ヘッダ解析処理の改善対応	K.Naono		02.02.20
#include "SXFPart21FileReadC.h"
#include "SXFTableManagerC.h"
#include "SXFReadFeatureManagerC.h"
#include "SXFErrorMsgC.h"
#include "SXFComInfoC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

//---------------------------------------------------------
//	All Rights Reserved,(c)JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//	１．機能概要
//		コンストラクタ
//
//	２．インターフェイス
//		void	SXFPart21FileReadC::SXFPart21FileReadC()
//		
//	引数	：	型		引数名				内容
//		なし
//      
//	復帰値：なし
//
//	３．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//---------------------------------------------------------
SXFPart21FileReadC::SXFPart21FileReadC()
{
    //initialization
    m_HeadSection.Level=-1;
    m_HeadSection.Mode=-1;
    m_HeadSection.Author.Empty();
    m_HeadSection.FileName.Empty();
    m_HeadSection.Organization.Empty();
    m_HeadSection.Version.Empty();
    m_HeadSection.OriginatingSystem.Empty();
    ClearInstanceData();
    m_File = NULL;
    m_TableManager = NULL;
	m_ReadFeatureManager = NULL;
	m_ComInfo = NULL;
  
}

//---------------------------------------------------------
//	All Rights Reserved,(c)JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//	１．機能概要
//		コンストラクタ
//
//	２．インターフェイス
//		void	SXFPart21FileReadC::SXFPart21FileReadC(
//								SXFTableManagerC* IN_TableManager,
//								SXFReadFeatureManagerC* IN_ReadFeatureManager,
//								SXFComInfoC* IN_ComInfo)
//
//	引数：型				引数名				内容
//		SXFTableManagerC*	IN_TableManager		定義ﾃｰﾌﾞﾙ管理ｵﾌﾞｼﾞｪｸﾄのｱﾄﾞﾚｽ
//		SXFReadFeatureManagerC* IN_ReadFeatureManager	Read管理ｵﾌﾞｼﾞｪｸﾄのｱﾄﾞﾚｽ
//		SXFComInfoC*		IN_ComInfo			共通情報ｵﾌﾞｼﾞｪｸﾄのｱﾄﾞﾚｽ
//      
//	復帰値：なし
//
//	３．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//---------------------------------------------------------
SXFPart21FileReadC::SXFPart21FileReadC(SXFTableManagerC* IN_TableManager,
							   SXFReadFeatureManagerC* IN_ReadFeatureManager,
                               SXFComInfoC* IN_ComInfo)
{
    //initialization
    m_HeadSection.Level=-1;
    m_HeadSection.Mode=-1;
    m_HeadSection.Author.Empty();
    m_HeadSection.FileName.Empty();
    m_HeadSection.Organization.Empty();
    m_HeadSection.Version.Empty();
    m_HeadSection.OriginatingSystem.Empty();
    ClearInstanceData();
    m_File = NULL;
    m_TableManager = IN_TableManager; 
	m_ReadFeatureManager = IN_ReadFeatureManager;
	m_ComInfo = IN_ComInfo;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//  １．機能概要
//      デストラクタ
//
//  ２．インターフェース
//		void	SXFPart21FileReadC::~SXFPart21FileReadC()
//		
//	引数	：	型		引数名				内容
//		なし
//      
//	復帰値：なし
//
//  ３．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//
//------------------------------------------------------------

SXFPart21FileReadC::~SXFPart21FileReadC()
{
    if(m_File != NULL)
        delete m_File;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//  １．機能概要
//      ファイルのオープン
//
//  ２．インターフェース
//      int OpenPart21ReadFile( LPCTSTR IN_FileName )
//	    引数: 型			  引数名			    内容
//            LPCTSTR         IN_FileName          FileName
//      
//	    復帰値	
//            正常時： 1: OK
//			  異常時： 負(エラーコード)
//
//  ３．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//		更新		2002.02.20	K.Naono		ヘッダ解析処理の改善対応
//
//------------------------------------------------------------
int SXFPart21FileReadC::OpenPart21ReadFile( LPCTSTR IN_FileName )
{
	int result_readinstance;

    TRY
    {
//	MOD(S)	ヘッダ解析処理の改善対応	K.Naono		02.02.20
//		m_File = new SXFPart21FileC(IN_FileName,
		m_File = new SXFPart21FileExC(IN_FileName,
//	MOD(E)	ヘッダ解析処理の改善対応	K.Naono		02.02.20
								CFile::modeRead | CFile::shareExclusive);
    }
    CATCH( CFileException, e )
    {
		/* 指定されたファイルまたはディレクトリが見つかりません */
        if (e->m_cause == CFileException::fileNotFound ){
			SXFErrorMsgC::RecordError(SXFFILEDIRECTORY,"",1,IN_FileName);
			return SXFFILEDIRECTORY;
        }
		/* STEPファイルのオープンに失敗しました */
		else{
			SXFErrorMsgC::RecordError(SXFNOTOPENSTEPFILE,"",1,IN_FileName);
			return SXFNOTOPENSTEPFILE;
        }

    }
    END_CATCH
    
    int result_readhead = m_File->ReadHeadSection(&m_HeadSection);
    if (result_readhead == -3){
		////can not find header
		SXFErrorMsgC::RecordError(SXFNOTHEADER,"",1,IN_FileName);
		return SXFNOTHEADER;
    }
	if (result_readhead < 0 ){
		////,ファイルのヘッダ情報が読取れません。
		SXFErrorMsgC::RecordError(SXFHEADERERR,"",0);
		result_readhead = 0;
    }

	/* SCADEC以外のﾌｧｲﾙ */
	if (result_readhead == 0){
		m_HeadSection.Author="";
		m_HeadSection.FileName=IN_FileName;
		m_HeadSection.Level = 2;
		m_HeadSection.Mode = 2;
		m_HeadSection.Organization="";
		m_HeadSection.OriginatingSystem="";
		m_HeadSection.Version="";
		m_HeadSection.scadec_flg = -1;
	}
	/** ファイルの情報を登録 **/
	int ret = m_ComInfo->SetFileInfo(m_HeadSection);
	if (ret < 0)
		return ret;
    while(1){
        result_readinstance = m_File->ReadInstance(&m_InstanceData);
        if ( result_readinstance ==1 ){
			switch (m_InstanceData.InstanceMode){
				case SXF_IM_FT_ENTITY:
					RegisterFeatureEntity(&m_InstanceData);
					break;
				case SXF_IM_FT_TABLE:
					RegisterFeatureTable(&m_InstanceData);
					break;
				case SXF_IM_FT_ASSEMBLY:
					RegisterFeatureAssembly(&m_InstanceData);
					break;
				default:
					break;
			}
			ClearInstanceData();//init instance data
		}
        else if(result_readinstance == -2)
			//found syntax error
			continue;			//next line 
		else if(  result_readinstance == -4  ){
			//can not to find a feature instance in this file
			SXFErrorMsgC::RecordError(SXFERRORFILE,"",0);
			return SXFERRORFILE;
		}
		else if(result_readinstance == -1)
			//if end of file then break while(1) and return TRUE
			break;
		else
			break;
    }
    return 1;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//  １．機能概要
//      ファイルのクローズ
//
//  ２．インターフェース
//      void ClosePart21ReadFile(  )
//
//	    引数:	型			  引数名			    内容
//				なし
//      
//	    復帰値	
//				なし
//
//  ３．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//
//------------------------------------------------------------
void SXFPart21FileReadC::ClosePart21ReadFile(  )
{
    m_File->Close();
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//  １．機能概要
//      Feature Comment Entityの登録
//
//  ２．インターフェース
//      BOOL RegisterFeatureEntity(INSTANCEDATA *IN_Instance)
//
//	    引数: 型			  引数名			    内容
//            INSTANCEDATA*   IN_Instance          Instanceの
//                                                 構造体のアドレス 
//	    復帰値	
//            正常時： TRUE
//			  異常時： FALSE
//
//  ３．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//
//------------------------------------------------------------
BOOL SXFPart21FileReadC::RegisterFeatureEntity(INSTANCEDATA *IN_Instance)
{
    if(! m_ReadFeatureManager->RegisterFeatureEntity(IN_Instance))
        return FALSE;
    return TRUE;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//  １．機能概要
//      Feature Comment Tableの登録
//
//  ２．インターフェース
//      BOOL RegisterFeatureTable(INSTANCEDATA *IN_Instance)
//
//	    引数: 型			  引数名			    内容
//            INSTANCEDATA*   IN_Instance          Instanceの
//                                                 構造体のアドレス 
//	    復帰値	
//            正常時： TRUE
//			  異常時： FALSE
//
//  ３．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//
//------------------------------------------------------------
BOOL SXFPart21FileReadC::RegisterFeatureTable(INSTANCEDATA *IN_Instance)
{
    if(! m_TableManager->RegisterFeatureTable(IN_Instance))
        return FALSE;
    return TRUE;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//  １．機能概要
//      Feature Assembly Instanceの登録
//
//  ２．インターフェース
//      BOOL RegisterFeatureAssembly(INSTANCEDATA *IN_Instance)
//
//	    引数: 型			  引数名			    内容
//            INSTANCEDATA*   IN_Instance          Instanceの
//                                                 構造体のアドレス 
//	    復帰値	
//            正常時： TRUE
//			  異常時： FALSE
//
//  ２．インターフェース
//
//  ３．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//
//------------------------------------------------------------
BOOL SXFPart21FileReadC::RegisterFeatureAssembly(INSTANCEDATA *IN_Instance)
{
    if(! m_ReadFeatureManager->RegisterFeatureAssembly(IN_Instance))
        return FALSE;
    return TRUE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//  １．機能概要
//      Head Section Dataの取得
//
//  ２．インターフェース
//      void GetHeadData(int *OUT_Level,
//                       int *OUT_Mode,
//                       char *OUT_FileName,
//                       char *OUT_Author,
//                       char *OUT_Organization,
//                       char *OUT_Version,
//                       char *OUT_OriginatingSystem )
//
//	    引数: 型			  引数名					内容
//            int*            OUT_Level					レベル
//            int*            OUT_Mode					モード
//            char*           OUT_FileName				ファイル名
//            char*           OUT_Author				ファイル作成者
//            char*           OUT_Organization			作成者所属
//            char*           OUT_Version				共通ライブラリバージョン
//            char*           OUT_OriginatingSystem		トランスレータ名
//
//	    復帰値:	
//            なし
//
//  ３．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//
//------------------------------------------------------------
void SXFPart21FileReadC::GetHeadData(int *OUT_Level,
                                     int *OUT_Mode,
                                     char *OUT_FileName,
                                     char *OUT_Author,
                                     char *OUT_Organization,
                                     char *OUT_Version,
                                     char *OUT_OriginatingSystem )
{
    *OUT_Level = m_HeadSection.Level;
    *OUT_Mode = m_HeadSection.Mode;

//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
	//strcpy(OUT_FileName,m_HeadSection.FileName);
	//strcpy(OUT_Author,m_HeadSection.Author);
	//strcpy(OUT_Organization,m_HeadSection.Organization);
	//strcpy(OUT_Version,m_HeadSection.Version);
	//strcpy(OUT_OriginatingSystem,m_HeadSection.OriginatingSystem);

	// 各項目のバッファサイズは仕様書より取得
	strcpy_s(OUT_FileName, 257,m_HeadSection.FileName);
	strcpy_s(OUT_Author, 257,m_HeadSection.Author);
	strcpy_s(OUT_Organization, 257,m_HeadSection.Organization);
	strcpy_s(OUT_Version, 257,m_HeadSection.Version);
	strcpy_s(OUT_OriginatingSystem, 257,m_HeadSection.OriginatingSystem);
//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileReadC.cpp	              作成日：2000.03.26
//
//  １．機能概要
//      Clear current instance data struct
//
//  ２．インターフェース
//       void ClearInstanceData()
//
//		引数	：	型		引数名				内容
//					なし
//
//  ３．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//
//------------------------------------------------------------
void SXFPart21FileReadC::ClearInstanceData()
{
    //init  m_InstanceData;
    m_InstanceData.InstanceMode = 0;
    m_InstanceData.InstanceID = 0;
    m_InstanceData.EntityName.Empty();
    m_InstanceData.EntityData.Empty();
}
