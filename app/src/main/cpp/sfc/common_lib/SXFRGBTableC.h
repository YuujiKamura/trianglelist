/**
	All Rights Reserved,(c) JACIC 2001
	SXFRGBTableC.cpp                作成日：2000.04.29

	１．クラス概要
		色テーブル情報を管理する

	２．履歴
	    履歴番号    日付け         担当者		概要
		作成		2000.4.29     宮園(FQS)
**/
#ifndef _SXFRGBTABLEC_H_
#define _SXFRGBTABLEC_H_

#include <math.h>
#include <string.h>
#include <afxtempl.h>
#include "SXFDefaultRGBC.h"

typedef CArray<SXFDefaultRGBC,SXFDefaultRGBC&> SXFDEFAULTCOLORTABLE;

class SXFRGBTableC{
public:
	SXFRGBTableC();
	~SXFRGBTableC();
	BOOL GetColorCode(const char*,int&);
private:
	SXFDEFAULTCOLORTABLE m_DefaultColorTable;		/* 色ﾃｰﾌﾞﾙ */

};

#endif//_SXFRGBTABLEC_H_
