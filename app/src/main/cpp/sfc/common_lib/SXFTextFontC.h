//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTextFontC.h                        作成日：2000.04.16
//
//	１．クラス概要
//		TextFont Instance Table Dataを管理する
// 
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.16     周
//
//------------------------------------------------------------
#ifndef _SXFTEXTFONTC_H_
#define _SXFTEXTFONTC_H_

#include <afx.h>

class SXFTableMapManagerC;

class SXFTextFontC
{
public:
	SXFTextFontC(  );
	virtual ~SXFTextFontC();

    int						m_Code;				/* 文字ﾌｫﾝﾄｺｰﾄﾞ */
    int						m_ID;				/* ｲﾝｽﾀﾝｽID */
    CString					m_Name;				/* 文字ﾌｫﾝﾄ名 */
    SXFTableMapManagerC*	m_MappingManager;	/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ */
};

#endif
