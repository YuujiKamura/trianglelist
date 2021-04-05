//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPreDefColorC.h                     作成日：2000.04.16
//
//	１．クラス概要
//		PreDefColor Instance Table Dataを管理する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.16     周
//
//------------------------------------------------------------
#ifndef _SXFPREDEFCOLORC_H_
#define _SXFPREDEFCOLORC_H_

#include <afx.h>

class SXFTableMapManagerC;

class SXFPreDefColorC  
{
public:
	SXFPreDefColorC(  );
	virtual ~SXFPreDefColorC();
	BOOL SetName(LPCTSTR IN_Name);

    int						m_Code;				/* 色ｺｰﾄﾞ */
    int						m_ID;				/* ｲﾝｽﾀﾝｽID */
    CString					m_Name;				/* 色名 */
    SXFTableMapManagerC*	m_MappingManager;	/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ */

};

#endif
