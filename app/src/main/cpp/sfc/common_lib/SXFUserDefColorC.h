//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFUserDefColorC.h                    作成日：2000.04.10
//
//	１．クラス概要
//      UserDefColor Instance Table Dataを管理する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
#ifndef _SXFUSERDEFCOLORC_H_
#define _SXFUSERDEFCOLORC_H_

#include <afx.h>

class SXFTableMapManagerC;

class SXFUserDefColorC
{
public:
	SXFUserDefColorC(  );
	virtual ~SXFUserDefColorC();

    int						m_Code;				/* 色ｺｰﾄﾞ */
    int						m_ID;				/* ｲﾝｽﾀﾝｽID */
    CString					m_Name;				/* 色名 */
    int						R,G,B;				/* RGB値 */
    SXFTableMapManagerC*	m_MappingManager;	/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ */
};

#endif
