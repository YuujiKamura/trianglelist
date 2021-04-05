/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreWidthC.cpp                作成日：2000.04.29

	１．クラス概要
		線幅テーブル情報を管理する

	２．履歴
	    履歴番号    日付け         担当者		概要
		作成		2000.4.29     宮園(FQS)
**/
#include "stdafx.h"
#include "SXFDefaultPreWidthC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreWidthC.cpp                作成日：2000.04.29

  １．機能概要
      クラス構築

  ２．インターフェース
		void	SXFPreWidthTableC::SXFPreWidthTableC()
		
	引数	：	型		引数名				内容
		なし

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFDefaultPreWidthC::SXFDefaultPreWidthC()
{

}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreWidthC.cpp                作成日：2000.04.29

  １．機能概要
      デストラクタ

  ２．インターフェース
		void	SXFPreWidthTableC::~SXFPreWidthTableC()
		
	引数	：	型		引数名				内容
		なし

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFDefaultPreWidthC::~SXFDefaultPreWidthC()
{

}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreWidthC.cpp                作成日：2000.04.29

  １．機能概要
      クラス構築

  ２．インターフェース
		void	SXFPreWidthTableC::SXFPreWidthTableC(int in_code,
													double in_width)
		
	引数	：	型		引数名		内容
				int		in_code		線幅コード
				double	in_width	線幅

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFDefaultPreWidthC::SXFDefaultPreWidthC(int in_code,double in_width){
	m_Code = in_code;
    m_Width = in_width;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreWidthC.cpp                作成日：2000.04.29

  １．機能概要
      線幅の取得

  ２．インターフェース
		void	SXFPreWidthTableC::GetWidth(double& out_width)
		
	引数	：	型		引数名		内容
				double&	out_width	線幅

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
void SXFDefaultPreWidthC::GetWidth(double& out_width){
	out_width = m_Width;
}
