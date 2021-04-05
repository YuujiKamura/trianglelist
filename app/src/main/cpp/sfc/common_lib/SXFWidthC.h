//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWidthC.h                           作成日：2000.04.16
//
//	１．クラス概要
//		線幅の構造体、コード、インスタンスＩＤなどを保持するクラス
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.16     周
//
//------------------------------------------------------------
#ifndef _SXFWIDTHC_H_
#define _SXFWIDTHC_H_

class SXFTableMapManagerC;

class SXFWidthC  
{
public:
	SXFWidthC(  );
	virtual ~SXFWidthC();
	int InquireCode(double IN_Width, double IN_Eps);

    int						m_Code;				/* 線幅ｺｰﾄﾞ */
    int						m_ID;				/* ｲﾝｽﾀﾝｽID */
    double					m_Width;			/* 線幅 */
    SXFTableMapManagerC*	m_MappingManager;	/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ */
};

#endif
