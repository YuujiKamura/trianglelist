//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPreDefFontC.h                     作成日：2000.04.16
//
//	１．クラス概要
//		PreDefFont Instance Table Dataを管理する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.16     周
//
//------------------------------------------------------------
#ifndef _SXFPREDEFFONTC_H_
#define _SXFPREDEFFONTC_H_

#include <afx.h>

class SXFTableMapManagerC;

class SXFPreDefFontC  
{
public:
	SXFPreDefFontC( );
	virtual ~SXFPreDefFontC();
    BOOL SetName(LPCTSTR IN_Name);

public:
    int						m_Code;				/* 線種ｺｰﾄﾞ */
    int						m_ID;				/* ｲﾝｽﾀﾝｽID */
    CString					m_Name;				/* 線種名 */
    SXFTableMapManagerC*	m_MappingManager;	/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ */
};

#endif
