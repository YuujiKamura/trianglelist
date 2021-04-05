//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureAssemblyC.h                  作成日：2000.04.20
//
//	１．クラス概要
//      Feature Assemblyの親子関係を管理する
//      
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.20     周        なし
//
//------------------------------------------------------------

#if !defined(AFX_SXFFEATUREASSEMBLYC_H__7A7CDA82_1A22_11D4_A76E_00000EB03267__INCLUDED_)
#define AFX_SXFFEATUREASSEMBLYC_H__7A7CDA82_1A22_11D4_A76E_00000EB03267__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include <afxtempl.h>

#include "SXFReadMapManagerC.h"
#include "SXFFeatureChildC.h"

typedef CTypedPtrList<CPtrList,SXFFeatureChildC*> SXFCHILDLIST;

class SXFFeatureAssemblyC  
{
public:
	SXFFeatureAssemblyC();
	virtual ~SXFFeatureAssemblyC();

	int					AssemCurveFlag;		/* 複合曲線表示／非表示ﾌﾗｸﾞ */
	int					AssemSheetX;		/* 用紙ｻｲｽﾞ横長 */
	int					AssemSheetY;		/* 用紙ｻｲｽﾞ縦長 */
	int					AssemSheetOrient;	/* 縦／横区分 */
	int					AssemSheetType;		/* 用紙ｻｲｽﾞ種別 */
	int					AssemSymbolType;	/* 複合図形種別 */
    int					AssemblyID;			/* ｱｾﾝﾌﾞﾘﾌｨｰﾁｬID */
    int					AssemblyCode;		/* ｱｾﾝﾌﾞﾘｺｰﾄﾞ */
	int					m_CurveColour;		/* 複合曲線色ｺｰﾄﾞ */
	int					m_CurveType;		/* 複合曲線線種ｺｰﾄﾞ */
	int					m_CurveWidth;		/* 複合曲線線幅ｺｰﾄﾞ */
	int					m_SfigLocateFlag;	/* 複合図形配置ﾌﾗｸﾞ */
											/* (未配置:-1 配置済:1) */
	BOOL				m_AssemblyErrFlag;	/* ｱｾﾝﾌﾞﾘﾌｨｰﾁｬｴﾗｰﾌﾗｸﾞ */
											/* (正常:TRUE 異常:FALSE) */
    CString				AssemblyName;		/* ｱｾﾝﾌﾞﾘ名 */
    SXFReadMapManagerC*	AssemblyMapManager;	/* ｱｾﾝﾌﾞﾘﾌｨｰﾁｬMapping管理ｵﾌﾞｼﾞｪｸﾄ */
    SXFCHILDLIST*		ChildList;			/* 一般要素ﾃｰﾌﾞﾙ */
};

#endif // !defined(AFX_SXFFEATUREASSEMBLYC_H__7A7CDA82_1A22_11D4_A76E_00000EB03267__INCLUDED_)
