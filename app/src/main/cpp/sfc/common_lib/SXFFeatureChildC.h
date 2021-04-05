//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureChildC.h                   作成日：2000.04.20
//
//	１．クラス概要
//      Feature Child Dataを管理する
//      
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.20    周
//
//------------------------------------------------------------

#if !defined(AFX_SXFFEATURECHILDC_H__7A7CDA81_1A22_11D4_A76E_00000EB03267__INCLUDED_)
#define AFX_SXFFEATURECHILDC_H__7A7CDA81_1A22_11D4_A76E_00000EB03267__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

class SXFReadMapManagerC;

class SXFFeatureChildC  
{
public:
	SXFFeatureChildC();
	virtual ~SXFFeatureChildC();

    int					ChildID;		/* 一般要素ﾌｨｰﾁｬのｲﾝｽﾀﾝｽID */
    SXFReadMapManagerC*	ChildMapManager;/* 一般要素ﾌｨｰﾁｬMapping管理ｵﾌﾞｼﾞｪｸﾄ */
};

#endif // !defined(AFX_SXFFEATURECHILDC_H__7A7CDA81_1A22_11D4_A76E_00000EB03267__INCLUDED_)
