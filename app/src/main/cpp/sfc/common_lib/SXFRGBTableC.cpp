/**
	All Rights Reserved,(c) JACIC 2001
	SXFRGBTableC.cpp                作成日：2000.04.29

	１．クラス概要
		色テーブル情報を管理する

	２．履歴
	    履歴番号    日付け         担当者		概要
		作成		2000.4.29     宮園(FQS)
**/
#include "stdafx.h"
#include <afx.h>
#include "SXFRGBTableC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/**
	All Rights Reserved,(c) JACIC 2001
	SXFRGBTableC.cpp                作成日：2000.04.29

  １．機能概要
      コンストラクタ

  ２．インターフェース
		void	SXFRGBTableC::SXFRGBTableC()
		
	引数	：	型		引数名				内容
		なし

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFRGBTableC::SXFRGBTableC(){
	m_DefaultColorTable.Add( SXFDefaultRGBC( 1,"black"     ,  0,  0,  0)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC( 2,"red"       ,255,  0,  0)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC( 3,"green"     ,  0,255,  0)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC( 4,"blue"      ,  0,  0,255)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC( 5,"yellow"    ,255,252,  0)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC( 6,"magenta"   ,255,  0,255)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC( 7,"cyan"      ,  0,255,255)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC( 8,"white"     ,255,255,255)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC( 9,"deeppink"  ,192,  0,128)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC(10,"brown"     ,192,128, 64)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC(11,"orange"    ,255,128,  0)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC(12,"lightgreen",128,192,128)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC(13,"lightblue" ,  0,128,255)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC(14,"lavender"  ,128, 64,255)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC(15,"lightgray" ,192,192,192)  );
	m_DefaultColorTable.Add( SXFDefaultRGBC(16,"darkgray"  ,128,128,128)  );
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFRGBTableC.cpp                作成日：2000.04.29

  １．機能概要
      デストラクタ

  ２．インターフェース
		void	SXFRGBTableC::~SXFRGBTableC()
		
	引数	：	型		引数名				内容
		なし

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
SXFRGBTableC::~SXFRGBTableC(){
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFRGBTableC.cpp                作成日：2000.04.29

  １．機能概要
      色名から色コードを取得する

  ２．インターフェース
		int	SXFRGBTableC::GetColorCode(const char* name,int& code)
		
	引数	：	型		引数名		内容
				char*	name		色名
				int&	code		色コード

	復帰値
			TRUE	色名に対応するRGB値あり
			FALSE	色名に対応するRGB値なし

  ３．履歴
	    履歴番号    日付け			担当者		 概要
	    作成		2000.04.29      宮園(FQS)
**/
BOOL SXFRGBTableC::GetColorCode(const char* name, int& code){
	char* color_name;
	for (int i= 0; i < m_DefaultColorTable.GetSize(); i++){
		m_DefaultColorTable[i].GetName(color_name);
		if (strcmp(color_name, name) == 0){
			code = m_DefaultColorTable[i].GetCode();
			return TRUE;
		}
	}
	return FALSE;
}
