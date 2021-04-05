// SXFDefaultPreWidthC.h: SXFDefaultPreWidthC クラスのインターフェイス
//
//////////////////////////////////////////////////////////////////////
/**
	All Rights Reserved,(c) JACIC 2001
	SXFWidthTableC.cpp                作成日：2000.04.29

	１．クラス概要
		線幅テーブル情報を管理する

	２．履歴
	    履歴番号    日付け         担当者		概要
		作成		2000.4.29     宮園(FQS)
**/

#if !defined(AFX_SXFDEFAULTPREWIDTHC_H__1F511811_020C_11D5_A3BC_009027565E9B__INCLUDED_)
#define AFX_SXFDEFAULTPREWIDTHC_H__1F511811_020C_11D5_A3BC_009027565E9B__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class SXFDefaultPreWidthC  
{
public:
	SXFDefaultPreWidthC();
	virtual ~SXFDefaultPreWidthC();
	SXFDefaultPreWidthC(int in_code,double in_width);
	void GetWidth(double& out_width);

	int m_Code;			/* 線幅ｺｰﾄﾞ */
	double m_Width;		/* 線幅 */
};

#endif // !defined(AFX_SXFDEFAULTPREWIDTHC_H__1F511811_020C_11D5_A3BC_009027565E9B__INCLUDED_)
