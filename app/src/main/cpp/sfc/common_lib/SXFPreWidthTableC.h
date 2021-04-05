/**
	All Rights Reserved,(c) JACIC 2001
	SXFWidthTableC.h                作成日：2000.04.29

	１．クラス概要
		線幅テーブル情報を管理する

	２．履歴
	    履歴番号    日付け         担当者		概要
		作成		2000.4.29     宮園(FQS)
**/
#ifndef _SXFPREWIDTHTABLEC_H_
#define _SXFPREWIDTHTABLEC_H_

#include <afxtempl.h>
#include "SXFDefaultPreWidthC.h"

typedef CArray<SXFDefaultPreWidthC, SXFDefaultPreWidthC&> SXFPREWIDTHTABLE;

class SXFPreWidthTableC  
{
public:
	SXFPreWidthTableC();
	~SXFPreWidthTableC();
    BOOL GetPreWidth(int in_code,double& out_width);
    int GetCode(double IN_Width,double IN_Eps);
private:
    SXFPREWIDTHTABLE m_DefaultPreWidthTable;		/* 線幅ﾃｰﾌﾞﾙ */
};

#endif // _SXFPREWIDTHTABLEC_H_
