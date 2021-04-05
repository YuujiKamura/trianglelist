//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFLayerC.h                           作成日：2000.04.16
//
//	１．クラス概要
//      Layer Instance Table Dataを管理する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.16     周
//
//------------------------------------------------------------
#ifndef _SXFLAYERC_H_
#define _SXFLAYERC_H_

#include <afx.h>

class SXFTableMapManagerC;

class SXFLayerC  
{
public:
	SXFLayerC();
	virtual ~SXFLayerC();
	SXFLayerC& operator=(const SXFLayerC &IN_Layer);

    int						m_Code;				/* ﾚｲﾔｺｰﾄﾞ */
    int						m_ID;				/* ｲﾝｽﾀﾝｽID */
    int						m_LfFlag;			/* 表示／非表示ﾌﾗｸﾞ */
    CString					m_Name;				/* ﾚｲﾔ名 */
    SXFTableMapManagerC*	m_MappingManager;	/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ */
};

#endif
