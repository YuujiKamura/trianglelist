/**
	All Rights Reserved,(c) JACIC 2001
	SXFWriteMappingManagerC.cpp								作成日：2000.4.14

	１．クラス概要
	Write時のフィーチャオブジェクトとMappingオブジェクトの管理を行うクラス。

	２．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/

#include "stdafx.h"
#include "SXFWriteMappingManagerC.h"
#include "SXFWriteManagerC.h"
#include "SXFFeatureC.h"
#include "SXFPart21FileWriteC.h"
#include "SXFInstanceIDC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/**
	All Rights Reserved,(c) JACIC 2001
	SXFWriteMappingManagerC.cpp								作成日：2000.4.14

	１．機能概要
	コンストラクタ

	２．インターフェース
		void	SXFWriteMappingManagerC::SXFWriteMappingManagerC()
		
	引数	：	型		引数名				内容
		なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
SXFWriteMappingManagerC::SXFWriteMappingManagerC()
{
	m_AssemType = -1;
	m_Feature = NULL;
	m_AsemFeature = NULL;
	m_WriteFile = NULL;
	m_InstanceIDManager = NULL;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFWriteMappingManagerC.cpp								作成日：2000.4.14

	１．機能概要
	コンストラクタ

	２．インターフェース
		void	SXFWriteMappingManagerC::SXFWriteMappingManagerC(
											int IN_AssemType,
											 SXFPart21FileWriteC* IN_WriteFile,
											 SXFInstanceIDC* IN_InstanceIDManager)
		
	引数：型					引数名					内容
		  int					IN_AssemType			アセンブリ型
		  SXFPart21FileWriteC*	IN_WriteFile			ﾌｧｲﾙ出力ｵﾌﾞｼﾞｪｸﾄ
		  SXFInstanceIDC*		IN_InstanceIDManager	ｲﾝｽﾀﾝｽIDｵﾌﾞｼﾞｪｸﾄ

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
SXFWriteMappingManagerC::SXFWriteMappingManagerC(int IN_AssemType,
												 SXFPart21FileWriteC* IN_WriteFile,
												 SXFInstanceIDC* IN_InstanceIDManager)
{
	m_AssemType = IN_AssemType;
	m_WriteFile = IN_WriteFile;
	m_InstanceIDManager = IN_InstanceIDManager;
	m_AsemFeature = NULL;
	m_Feature = NULL;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFWriteMappingManagerC.cpp								作成日：2000.4.14

	１．機能概要
	デストラクタ

	２．インターフェース
		void	SXFWriteMappingManagerC::~SXFWriteMappingManagerC()
		
	引数	：	型		引数名				内容
		なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
SXFWriteMappingManagerC::~SXFWriteMappingManagerC()
{
	if (m_AsemFeature != NULL)
		delete m_AsemFeature;
	if (m_Feature != NULL)
		delete 	m_Feature;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFWriteMappingManagerC.cpp								作成日：2000.4.14

	１．機能概要
	構造体をSETする

	２．インターフェース
	void SetStructData(int IN_Asem_Type, LPVOID IN_Struct)

	引数:	型				引数名				内容
			int				IN_Asem_Type		アセンブリ型
			LPVOID			IN_Struct			構造体

	復帰値:
			なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
void SXFWriteMappingManagerC::SetStructData(int IN_Asem_Type, LPVOID IN_Struct)
{
	//アセンブリタイプのセット
	m_AssemType = IN_Asem_Type;
	switch(IN_Asem_Type){
		case 1://SHEET
			//構造体のセット
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(m_Sheet_Str.name,((Sheet_Struct*)IN_Struct)->name);
			strcpy_s(m_Sheet_Str.name,_countof(m_Sheet_Str.name),((Sheet_Struct*)IN_Struct)->name);
			m_Sheet_Str.orient = ((Sheet_Struct*)IN_Struct)->orient;
			m_Sheet_Str.type = ((Sheet_Struct*)IN_Struct)->type;
			m_Sheet_Str.x = ((Sheet_Struct*)IN_Struct)->x;
			m_Sheet_Str.y = ((Sheet_Struct*)IN_Struct)->y;
			break;
		case 2://subfigure
			//構造体のセット
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(m_Sfigorg_Str.name,((Sfigorg_Struct*)IN_Struct)->name);
			strcpy_s(m_Sfigorg_Str.name,_countof(m_Sfigorg_Str.name),((Sfigorg_Struct*)IN_Struct)->name);
			m_Sfigorg_Str.flag = ((Sfigorg_Struct*)IN_Struct)->flag;
			break;
		case 3://composite_curve
			//構造体のセット
			m_CompCurve_Str.color = ((Ccurve_Org_Struct*)IN_Struct)->color;
			m_CompCurve_Str.type = ((Ccurve_Org_Struct*)IN_Struct)->type;
			m_CompCurve_Str.line_width = ((Ccurve_Org_Struct*)IN_Struct)->line_width;
			m_CompCurve_Str.flag = ((Ccurve_Org_Struct*)IN_Struct)->flag;
			break;
	}
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFWriteMappingManagerC.cpp								作成日：2000.4.14

	１．機能概要
	アセンブリ要素のMapping

	２．インターフェース
	int MappingAssembly()

	引数:	型				引数名				内容
			なし

	復帰値:
			正常時：0
			異常時：負(error_code)

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
int SXFWriteMappingManagerC::MappingAssembly()
{
	INSTANCEDATA* assemfeature;
	int error_code;

	/* ｱｾﾝﾌﾞﾘﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ生成 */
	m_AsemFeature = new SXFFeatureC(m_InstanceIDManager);
	switch(m_AssemType){
		/** 用紙 **/
		case 1:
			/* ｱｾﾝﾌﾞﾘ構造体ｾｯﾄ */
			m_AsemFeature->SetAssemblyData(m_AssemType,&m_Sheet_Str);
			break;
		/** 複合図形 **/
		case 2:
			/* ｱｾﾝﾌﾞﾘ構造体ｾｯﾄ */
			m_AsemFeature->SetAssemblyData(m_AssemType,&m_Sfigorg_Str);
			break;
		/** 複合曲線 **/
		case 3:
			/* ｱｾﾝﾌﾞﾘ構造体ｾｯﾄ */
			m_AsemFeature->SetAssemblyData(m_AssemType,&m_CompCurve_Str);
			break;
	}
	/* ｲﾝｽﾀﾝｽﾃﾞｰﾀ取得 */
	assemfeature = m_AsemFeature->GetFeatureData();
	/* ｱｾﾝﾌﾞﾘﾌｨｰﾁｬｲﾝｽﾀﾝｽ出力 */
	error_code = m_WriteFile->WriteFeatureInstance(assemfeature);
	return error_code;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFWriteMappingManagerC.cpp								作成日：2000.4.14

	１．機能概要
	メンバのデータクリア

	２．インターフェース
	void ClearData()
	引数:	型				引数名				内容
			なし。
	復帰値	なし。

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
void SXFWriteMappingManagerC::ClearData()
{
	//////////////////
	//data clear
	//////////////////
	m_AssemType = -1;
	if (m_Feature){
		delete m_Feature;
		m_Feature=NULL;
	}
	if (m_AsemFeature){
		delete m_AsemFeature;
		m_AsemFeature=NULL;
	}
}
/**
	All Rights Reserved,(c) JACIC 2001
	SXFWriteMappingManagerC.cpp								作成日：2000.4.14

	１．機能概要
		フィーチャ要素のMapping

	２．インターフェース
	int MappingFeature(char *IN_FeatureType, LPVOID IN_Struct,int &OUT_Id1)
	引数:	型		引数名				内容
			char*	IN_FeatureType		フィーチャ型
			LPVOID	IN_Struct			構造体
			int&	OUT_Id1				一般要素のインスタンスID

	復帰値:
			正常時:0
			異常時:負(error_code)

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/
int SXFWriteMappingManagerC::MappingFeature(char *IN_FeatureType, LPVOID IN_Struct,int &OUT_Id1)
{
	INSTANCEDATA* feature;
	int error_code;

	/* フィーチャオブジェクト生成 **/
	m_Feature = new SXFFeatureC(m_InstanceIDManager);
	/* 構造体のセット */
	m_Feature->SetFeatureData(IN_FeatureType,IN_Struct);
	/* ｲﾝｽﾀﾝｽﾃﾞｰﾀ取得 */
	feature = m_Feature->GetFeatureData();
	/* ｱｾﾝﾌﾞﾘﾌｨｰﾁｬｲﾝｽﾀﾝｽ出力 */
	error_code = m_WriteFile->WriteFeatureInstance(feature);
	/* ｲﾝｽﾀﾝｽID取得 */
	OUT_Id1 = feature->InstanceID;
	return error_code;
}
