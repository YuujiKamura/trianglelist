/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreLineTypeC.cpp                作成日：2000.04.29

	１．クラス概要
		線種テーブル情報を管理する

	２．履歴
	    履歴番号    日付け         担当者		概要
		作成		2000.4.29     宮園(FQS)
**/
#include "stdafx.h"
#include <string.h>
#include "SXFDefaultPreLineTypeC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreLineTypeC.cpp                作成日：2000.04.29

  １．機能概要
      クラス構築

  ２．インターフェース
		void	SXFDefaultPreLineTypeC::SXFDefaultPreLineTypeC()
		
	引数	：	型		引数名				内容
				なし

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFDefaultPreLineTypeC::SXFDefaultPreLineTypeC()
{

}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreLineTypeC.cpp                作成日：2000.04.29

  １．機能概要
      クラス構築

  ２．インターフェース
	void	SXFDefaultPreLineTypeC::SXFDefaultPreLineTypeC(int in_code,
														const char* in_name)
		
	引数	：	型		引数名		内容
				int		in_code		線種コード
				char*	in_name		線種名

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFDefaultPreLineTypeC::SXFDefaultPreLineTypeC(int in_code, const char *in_name)
{
	m_Code = in_code;
//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
//	strcpy(m_Name,in_name);
	strcpy_s(m_Name,_countof(m_Name),in_name);
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreLineTypeC.cpp                作成日：2000.04.29

  １．機能概要
      デストラクタ

  ２．インターフェース
		void	SXFDefaultPreLineTypeC::~SXFDefaultPreLineTypeC()
		
	引数	：	型		引数名				内容
		なし

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFDefaultPreLineTypeC::~SXFDefaultPreLineTypeC()
{

}
    
/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreLineTypeC.cpp                作成日：2000.04.29

  １．機能概要
      線種名の取得

  ２．インターフェース
		void	SXFDefaultPreLineTypeC::GetName(char*& out_name)
		
	引数	：	型		引数名		内容
				char*	out_name	線種名

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
void SXFDefaultPreLineTypeC::GetName(char*& out_name)
{
	out_name = m_Name;
}    

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreLineTypeC.cpp                作成日：2000.04.29

  １．機能概要
      線種コードの取得

  ２．インターフェース
		int	SXFDefaultPreLineTypeC::GetCode()
		
	引数	：	型		引数名		内容
				なし
	復帰値
		int		m_Code		線種コード

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
int SXFDefaultPreLineTypeC::GetCode()
{
	return m_Code;
}
