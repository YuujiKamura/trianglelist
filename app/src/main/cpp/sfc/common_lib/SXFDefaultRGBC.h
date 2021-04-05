/**
	All Rights Reserved,(c) JACIC 2001
	SXFDefaultRGBC.h                  作成日：2000.04.29

	１．クラス概要
		色テーブル情報を管理する

	２．履歴
	    履歴番号    日付け         担当者		概要
		作成		2000.4.29     宮園(FQS)
**/

#if !defined(AFX_SXFDEFAULTRGBC_H__1F511813_020C_11D5_A3BC_009027565E9B__INCLUDED_)
#define AFX_SXFDEFAULTRGBC_H__1F511813_020C_11D5_A3BC_009027565E9B__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class SXFDefaultRGBC  
{
public:
	SXFDefaultRGBC();
	SXFDefaultRGBC(int code,
					char *name, 
					int red,
					int green,
					int blue);
	virtual ~SXFDefaultRGBC();
	void GetName(char*& name);
	int GetCode();
private:
    int m_Code;			/* 色ｺｰﾄﾞ */
	char m_Name[16];	/* 色名 */
	int m_Red;			/* R値 */
	int m_Green;		/* G値 */
	int m_Blue;			/* B値 */
};

#endif // !defined(AFX_SXFDEFAULTRGBC_H__1F511813_020C_11D5_A3BC_009027565E9B__INCLUDED_)
