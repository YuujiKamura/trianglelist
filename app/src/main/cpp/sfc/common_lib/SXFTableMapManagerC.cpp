/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.cpp								作成日：2000.4.14

	１．クラス概要
	定義テーブルフィーチャオブジェクトとMappingオブジェクトを管理するクラス

	２．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/

#include "stdafx.h"
#include "SXFTableManagerC.h"
#include "SXFTableMapManagerC.h"
#include "SXFTableFeatureC.h"
#include "SXFPreDefColorC.h"
#include "SXFLayerC.h"
#include "SXFPart21FileWriteC.h"
#include "SXFInstanceIDC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.cpp								作成日：2000.4.14

	１．機能概要
	コンストラクタ

	２．インターフェース
		void	SXFTableMapManagerC::SXFTableMapManagerC()
		
	引数	：	型		引数名				内容
		なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
SXFTableMapManagerC::SXFTableMapManagerC()
{
	m_TableFeature = NULL;
	m_LayerFeature = NULL;
	m_WriteFile = NULL;
	m_InstanceIDManager = NULL;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.cpp								作成日：2000.4.14

	１．機能概要
	コンストラクタ

	２．インターフェース

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
SXFTableMapManagerC::SXFTableMapManagerC(SXFPart21FileWriteC* IN_WriteFile,
											SXFInstanceIDC* IN_InstanceIDManager)
{
	m_TableFeature = NULL;
	m_LayerFeature = NULL;
	m_WriteFile = IN_WriteFile;
	m_InstanceIDManager = IN_InstanceIDManager;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.cpp								作成日：2000.4.14

	１．機能概要
		デストラクタ

	２．インターフェース

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
SXFTableMapManagerC::~SXFTableMapManagerC()
{
	if (m_TableFeature != NULL)
		delete m_TableFeature;
	if (m_LayerFeature != NULL)
		delete m_LayerFeature;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.cpp								作成日：2000.4.14

	１．機能概要
	構造体の生成

	２．インターフェース
	void SetStructData(int IN_type, LPVOID IN_Struct)

	引数:	型				引数名				内容
			int				IN_type				テーブル型
			LPVOID			IN_Struct			構造体
	復帰値:
			なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
void SXFTableMapManagerC::SetStructData(int IN_type, LPVOID IN_Struct)
{
	/* 定義ﾃｰﾌﾞﾙﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ生成 */
	m_TableFeature = new SXFTableFeatureC(m_InstanceIDManager);
	/* 定義ﾃｰﾌﾞﾙ構造体ｾｯﾄ */
	m_TableFeature->SetFeatureData(IN_type,IN_Struct);
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.cpp								作成日：2000.4.14

	１．機能概要
	インスタンスをファイルへ出力する

	２．インターフェース
	int WriteInstance(int* OUT_InstanceID)

	引数:	型		引数名			内容
			int*	OUT_InstanceID	ｲﾝｽﾀﾝｽID

	復帰値	正常時：0
			異常時：負(error_code)

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
int SXFTableMapManagerC::WriteInstance(int* OUT_InstanceID)
{
	INSTANCEDATA* tablefeature;
	*OUT_InstanceID = -1;
	int error_code;

	/* ｲﾝｽﾀﾝｽﾃﾞｰﾀ取得 */
	tablefeature = m_TableFeature->GetFeatureData();
	/* ｲﾝｽﾀﾝｽ出力 */
	error_code = m_WriteFile->WriteFeatureInstance(tablefeature);
	if (error_code)
		return error_code;
	/* ｲﾝｽﾀﾝｽID取得 */
	*OUT_InstanceID = tablefeature->InstanceID;
	return 0;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.cpp								作成日：2000.4.14

	１．機能概要
	既定義色の構造体の生成(READ)

	２．インターフェース
	void SetStructPrecolor(int IN_Type, LPVOID IN_Struct, SXFPreDefColorC IN_Precolor)

	引数:	型				引数名			内容
			int				IN_Type			テーブル型
			LPVOID			IN_Struct		構造体
			SXFPreDefColorC IN_Precolor		既定義色クラス

	復帰値	正常時：TRUE
			異常時：FALSE

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
void SXFTableMapManagerC::SetStructPrecolor(int IN_Type, 
											LPVOID IN_Struct, 
											SXFPreDefColorC IN_Precolor)
{
	/* 定義ﾃｰﾌﾞﾙﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ生成 */
	m_TableFeature = new SXFTableFeatureC(m_InstanceIDManager);
	/* 定義ﾃｰﾌﾞﾙ構造体ｾｯﾄ */
	m_TableFeature->SetFeatureData(IN_Type,IN_Struct);
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.cpp								作成日：2000.4.14

	１．機能概要
	レイヤフィーチャの構造体の生成(WRITE)

	２．インターフェース
	BOOL SXFTableMapManagerC::SetStructLayer(SXFLayerC *IN_Layer,int* OUT_InstanceID)

	引数:	型				引数名			内容
			SXFLayerC*		IN_Layer		レイヤクラス
			int*			OUT_InstanceID	インスタンスID
	復帰値:
			正常時：TRUE
			異常時：FALSE

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
BOOL SXFTableMapManagerC::SetStructLayer(SXFLayerC *IN_Layer,int* OUT_InstanceID){
	INSTANCEDATA* layerfeature;
	*OUT_InstanceID = -1;

	/* ﾚｲﾔﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ生成 */
	m_LayerFeature = new SXFTableFeatureC(m_InstanceIDManager);
	/* ﾚｲﾔ情報ｾｯﾄ */
	m_LayerFeature->SetLayerFeature(IN_Layer);
	/* ﾚｲﾔｲﾝｽﾀﾝｽﾃﾞｰﾀ取得 */
	layerfeature = m_LayerFeature->GetFeatureData();
	/* ﾚｲﾔｲﾝｽﾀﾝｽ出力 */
	if (!m_WriteFile->WriteFeatureInstance(layerfeature))
		return FALSE;
	/* ｲﾝｽﾀﾝｽID取得 */
	*OUT_InstanceID = layerfeature->InstanceID;
	return TRUE;
}


/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.cpp								作成日：2000.4.14

	１．機能概要
	構造体の生成(READ)

	２．インターフェース
	void SetFeatureData(int IN_Type, LPVOID IN_Class)

	引数:	型				引数名		内容
			int				IN_Type		定義テーブル型
			LPVOID			IN_Class	構造体

	復帰値	正常時：TRUE
			異常時：FALSE

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
void SXFTableMapManagerC::SetFeatureData(int IN_Type, LPVOID IN_Class)
{
	/* 定義ﾃｰﾌﾞﾙﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ生成 */
	m_TableFeature = new SXFTableFeatureC();
	/* 定義ﾃｰﾌﾞﾙ構造体ｾｯﾄ */
	m_TableFeature->SetStructData(IN_Type,IN_Class);
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.cpp								作成日：2000.4.14

	１．機能概要
	構造体の取得(READ)

	２．インターフェース
	LPVOID GetStructData()

	引数:	型				引数名		内容
			なし

	復帰値	構造体

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
LPVOID SXFTableMapManagerC::GetStructData()
{
	return m_TableFeature->GetStructData();
}
