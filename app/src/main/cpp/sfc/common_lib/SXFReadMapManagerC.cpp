/**
	All Rights Reserved,(c) JACIC 2001
	SXFReadMapManagerC.cpp								作成日：2000.4.25

	１．クラス概要
	Read時(フィーチャコメント）のフィーチャオブジェクトと
						Mappingオブジェクトの管理を行うクラス。


	２．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.25			宮園(FQS)			なし。

**/

#include "stdafx.h"
#include "SXFReadMapManagerC.h"
#include "SXFFeatureC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/**
	All Rights Reserved,(c) JACIC 2001
	SXFReadMapManagerC.cpp								作成日：2000.4.25

	１．機能概要
	コンストラクタ

	２．インターフェース
		void	SXFReadMapManagerC::SXFReadMapManagerC()
		
	引数	：	型		引数名				内容
		なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.25			宮園(FQS)			なし。

**/
SXFReadMapManagerC::SXFReadMapManagerC()
{
	m_Feature = NULL;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFReadMapManagerC.cpp								作成日：2000.4.25

	１．機能概要
	デストラクタ

	２．インターフェース
		void	SXFReadMapManagerC::~SXFReadMapManagerC()
		
	引数	：	型		引数名				内容
		なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.25			宮園(FQS)			なし。

**/
SXFReadMapManagerC::~SXFReadMapManagerC()
{
	if (m_Feature != NULL)
		delete m_Feature;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFReadMapManagerC.cpp								作成日：2000.4.25

	１．機能概要
	インスタンスデータの設定

	２．インターフェース
	BOOL SXFReadMapManagerC::SetFeatureData(INSTANCEDATA *IN_Instance)

	引数:	型				引数名				内容
			INSTANCEDATA*	IN_Instance			インスタンスデータ

	復帰値	正常終了：TRUE
			以上終了：FALSE

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.25			宮園(FQS)			なし。

**/
BOOL SXFReadMapManagerC::SetFeatureData(INSTANCEDATA *IN_Instance)
{
	/* ﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ生成 */
	m_Feature = new SXFFeatureC();
	/* ｲﾝｽﾀﾝｽﾃﾞｰﾀｾｯﾄ */
	return m_Feature->SetStructData(IN_Instance);
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFReadMapManagerC.cpp								作成日：2000.4.25

	１．機能概要
	一般要素フィーチャの構造体取得

	２．インターフェース
	LPVOID GetStructData(char* &OUT_TypeName)

	引数:	型			引数名				内容
			char*&		OUT_TypeName		フィーチャ型
			size_t		TypeNameSize		バッファサイズ

	復帰値	構造体

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.25			宮園(FQS)			なし。
	更新			2015.04.27			T.Matsushima		JPCERT/CC 指摘 脆弱性対応

**/
//LPVOID SXFReadMapManagerC::GetStructData(char* &OUT_TypeName)
LPVOID SXFReadMapManagerC::GetStructData(char* &OUT_TypeName,size_t TypeNameSize)
{
	return m_Feature->GetStructData(OUT_TypeName,TypeNameSize);
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFReadMapManagerC.cpp								作成日：2000.4.25

	１．機能概要
	アセンブリフィーチャの構造体取得

	２．インターフェース
	LPVOID GetAssemblyStructData()

	引数:	型				引数名				内容
			なし。

	復帰値	構造体

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.25			宮園(FQS)			なし。

**/
LPVOID SXFReadMapManagerC::GetAssemblyStructData()
{
	return m_Feature->GetAssemblyStructData();

}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFReadMapManagerC.cpp                   作成日：2001.03.21

  １．機能概要
      フィーチャ構造体返却済みフラグをONにする
      
  ２．インターフェース
      void SetReturnFlag()

	    引数: 型		引数名		内容
			  なし

	    復帰値	
			  なし

  ３．履歴
	    履歴番号    日付け         担当者    概要
      作成		2001.03.21     宮園

**/
void SXFReadMapManagerC::SetReturnFlag()
{
	m_Feature->SetReturnFlag();
}
