/**
	All Rights Reserved,(c) JACIC 2001
	SXFPreLineTypeTableC.h                作成日：2000.04.29

	１．クラス概要
		既定義線種テーブル情報を管理する

	２．履歴
	    履歴番号    日付け         担当者		概要
		作成		2000.4.29     宮園(FQS)
**/
#ifndef _SXFPRELINETYPETABLEC_H_
#define _SXFPRELINETYPETABLEC_H_

#include <string.h>
#include <afxtempl.h>
#include "SXFDefaultPreLineTypeC.h"

typedef CArray<SXFDefaultPreLineTypeC, SXFDefaultPreLineTypeC&> SXFDEFAULTPRELINETYPE;

class SXFPreLineTypeTableC{  

public:
	SXFPreLineTypeTableC();
	~SXFPreLineTypeTableC();
	BOOL GetPreLinetype(int in_code, char*& out_name);
	int GetPreCode(const char* in_name);
private:
	SXFDEFAULTPRELINETYPE m_DefaultPreLineTypeTable;	/* 色ﾃｰﾌﾞﾙ */
};

#endif // _SXFPRELINETYPETABLEC_H_
