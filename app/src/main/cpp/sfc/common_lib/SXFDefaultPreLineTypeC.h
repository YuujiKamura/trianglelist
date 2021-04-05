/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultPreLineTypeC.h                  作成日：2000.04.29

	１．クラス概要
		線種テーブル情報を管理する

	２．履歴
	    履歴番号    日付け         担当者		概要
		作成		2000.4.29     宮園(FQS)
**/
#if !defined(AFX_SXFDEFAULTPRELINETYPEC_H__1F511812_020C_11D5_A3BC_009027565E9B__INCLUDED_)
#define AFX_SXFDEFAULTPRELINETYPEC_H__1F511812_020C_11D5_A3BC_009027565E9B__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class SXFDefaultPreLineTypeC  
{
public:
	SXFDefaultPreLineTypeC();
	SXFDefaultPreLineTypeC(int in_code, const char *in_name);
	virtual ~SXFDefaultPreLineTypeC();
    void GetName(char*& out_name);
	int GetCode();
private:
    int m_Code;			/* 線種ｺｰﾄﾞ */
    char m_Name[128];	/* 線種名 */
};

#endif // !defined(AFX_SXFDEFAULTPRELINETYPEC_H__1F511812_020C_11D5_A3BC_009027565E9B__INCLUDED_)
