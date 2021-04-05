/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultRGBC.cpp                作成日：2000.04.29

	１．クラス概要
		色テーブル情報を管理する

	２．履歴
	    履歴番号    日付け         担当者		概要
		作成		2000.4.29     宮園(FQS)
**/
#include "stdafx.h"
#include <math.h>
#include <string.h>
#include "SXFDefaultRGBC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultRGBC.cpp                作成日：2000.04.29

  １．機能概要
      クラス構築

  ２．インターフェース
		void	SXFDefaultRGBC::SXFDefaultRGBC()
		
	引数	：	型		引数名				内容
		なし

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFDefaultRGBC::SXFDefaultRGBC()
{

}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultRGBC.cpp                作成日：2000.04.29

  １．機能概要
      デストラクタ

  ２．インターフェース
		void	SXFDefaultRGBC::~SXFDefaultRGBC()
		
	引数	：	型		引数名				内容
		なし

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFDefaultRGBC::~SXFDefaultRGBC()
{

}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultRGBC.cpp                作成日：2000.04.29

  １．機能概要
      クラス構築

  ２．インターフェース
   void	SXFDefaultRGBC::SXFDefaultRGBC(int code,
										char *name, 
										int red,
										int green,
										int blue);
		
	引数	：	型		引数名		内容
				int		code		色コード
				char*	name		色名 
				int		red			R値
				int		green		G値
				int		blue		B値

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFDefaultRGBC::SXFDefaultRGBC(int code,
								char *name, 
								int red,
								int green,
								int blue){
	m_Code = code;
//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
//	strcpy(m_Name,name);
	strcpy_s(m_Name,_countof(m_Name),name);
	m_Red  = red;
	m_Green = green;
	m_Blue = blue;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultRGBC.cpp                作成日：2000.04.29

  １．機能概要
      色コード取得

  ２．インターフェース
		int	SXFDefaultRGBC::GetCode()
		
	引数	：	型		引数名				内容
			なし
	復帰値
			int		m_Code		色コード

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
int SXFDefaultRGBC::GetCode(){
	return m_Code;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultRGBC.cpp                作成日：2000.04.29

  １．機能概要
      色名の取得

  ２．インターフェース
		void	SXFDefaultRGBC::GetName(char*& name)
		
	引数	：	型		引数名		内容
				char*&	name		色名

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
void SXFDefaultRGBC::GetName(char*& name){
	name = m_Name;
}
