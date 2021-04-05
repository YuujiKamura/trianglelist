/**
	All Rights Reserved,(c) JACIC 2001
	SXFRuleCheckC.h								作成日：2000.4.19

	１．クラス概要
	フィーチャのルールチェックを行う

	２．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.19			宮園(FQS)			なし。

**/

#if !defined(AFX_SXFRULECHECKC_H__266DAF61_1152_11D4_A76E_00000EB03267__INCLUDED_)
#define AFX_SXFRULECHECKC_H__266DAF61_1152_11D4_A76E_00000EB03267__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include <afx.h>
#include "SXFUserdefined_color_checkC.h"
#include "SXFLayer_checkC.h"
#include "SXFUsertype_checkC.h"	// ClassView によって追加されました。
#include "SXFWidth_checkC.h"	// ClassView によって追加されました。
#include "SXFSfigorg_checkC.h"	// ClassView によって追加されました。
#include "SXFCcurveorg_checkC.h"	// ClassView によって追加されました。
#include "SXFText_String_checkC.h"	// ClassView によって追加されました。
#include "SXFPreColour_checkC.h"	// ClassView によって追加されました。
#include "SXFPreType_checkC.h"	// ClassView によって追加されました。
#include "SXFArc_checkC.h"	// ClassView によって追加されました。
#include "SXFEllipse_checkC.h"	// ClassView によって追加されました。
#include "SXFEllipse_Arc_checkC.h"	// ClassView によって追加されました。
#include "SXFCircle_checkC.h"	// ClassView によって追加されました。
#include "SXFLinearDim_checkC.h"
#include "SXFPoint_Marker_checkC.h"	// ClassView によって追加されました。
#include "SXFSpline_checkC.h"	// ClassView によって追加されました。
#include "SXFClothoid_checkC.h"	// ClassView によって追加されました。
#include "SXFCurveDim_checkC.h"	// ClassView によって追加されました。
#include "SXFAngularDim_checkC.h"	// ClassView によって追加されました。
#include "SXFDiameterDim_checkC.h"	// ClassView によって追加されました。
#include "SXFRadiusDim_checkC.h"	// ClassView によって追加されました。
#include "SXFLabel_checkC.h"	// ClassView によって追加されました。
#include "SXFBalloon_checkC.h"	// ClassView によって追加されました。
#include "SXFExSymbol_checkC.h"	// ClassView によって追加されました。
#include "SXFSfigloc_checkC.h"	// ClassView によって追加されました。
#include "SXFExhatch_checkC.h"	// ClassView によって追加されました。
#include "SXFFillHatching_checkC.h"	// ClassView によって追加されました。
#include "SXFFillTiles_checkC.h"	// ClassView によって追加されました。
#include "SXFFillColour_checkC.h"	// ClassView によって追加されました。
#include "SXFPolyline_checkC.h"	// ClassView によって追加されました。
#include "SXFTextFont_checkC.h"	// ClassView によって追加されました。
#include "SXFLine_checkC.h"	// ClassView によって追加されました。
#include "SXFSheet_checkC.h"	// ClassView によって追加されました。
#include "SXFAttribute_checkC.h"	// ClassView によって追加されました。

class SXFTableManagerC;
class SXFWriteManagerC;
class SXFComInfoC;

class SXFRuleCheckC  
{
public:
	SXFRuleCheckC();
	virtual ~SXFRuleCheckC();
	int FeatureRuleCheck(int IN_AssemType,char* IN_Type,LPVOID IN_Struct);
	int AssemblyRuleCheck(int IN_Type,LPVOID IN_Struct);
	int TableRuleCheck(int IN_Type,LPVOID IN_Struct);

	CString					m_InstanceID;			/* ｲﾝｽﾀﾝｽID */
	SXFReadFeatureManagerC* m_ReadFeatureManager;	/* Read管理ｵﾌﾞｼﾞｪｸﾄ */
	SXFWriteManagerC*		m_WriteManager;			/* Write管理ｵﾌﾞｼﾞｪｸﾄ */
	SXFTableManagerC*		m_TableManager;			/* 定義ﾃｰﾌﾞﾙ管理ｵﾌﾞｼﾞｪｸﾄ */
	SXFComInfoC*			m_ComInfo;				/* 共通情報管理ｵﾌﾞｼﾞｪｸﾄ */
private:
	int						m_SfigFlag;				/* 複合図形種別 */
	SXFFillTiles_checkC		m_FillTiles_check;		/* ﾊｯﾁ(ﾊﾟﾀｰﾝ)ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFFillHatching_checkC	m_FillHatching_check;	/* ﾊｯﾁ(ﾕｰｻﾞ定義)ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFFillColour_checkC	m_FillColour_check;		/* ﾊｯﾁ(塗り)ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFExhatch_checkC		m_Exhatch_check;		/* ﾊｯﾁ(外部定義)ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFSfigloc_checkC		m_Sfigloc_check;		/* 複合図形配置ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFExSymbol_checkC		m_ExSymbol_check;		/* 既定義ｼﾝﾎﾞﾙﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFBalloon_checkC		m_Balloon_check;		/* ﾊﾞﾙｰﾝﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFLabel_checkC			m_Label_check;			/* 引出し線ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFRadiusDim_checkC		m_RadiusDim_check;		/* 半径寸法ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFDiameterDim_checkC	m_DiameterDim_check;	/* 直径寸法ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFCurveDim_checkC		m_CurveDim_check;		/* 弧長寸法ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFAngularDim_checkC	m_AngularDim_check;		/* 角度寸法ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFSpline_checkC		m_Spline_check;			/* ｽﾌﾟﾗｲﾝﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFClothoid_checkC		m_Clothoid_check;		/* ｸﾛｿｲﾄﾞﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFPolyline_checkC		m_Polyline_check;		/* 折線ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFPoint_Marker_checkC	m_Point_Marker_check;	/* 点ﾏｰｶﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFLinearDim_checkC		m_LinearDim_check;		/* 直線寸法ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFCircle_checkC		m_Circle_check;			/* 円ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFEllipse_Arc_checkC	m_Ellipse_Arc_check;	/* 楕円弧ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFEllipse_checkC		m_Ellipse_check;		/* 楕円ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFArc_checkC			m_Arc_check;			/* 円弧ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFTextFont_checkC		m_TextFont_check;		/* 文字ﾌｫﾝﾄﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFPreType_checkC		m_PreType_check;		/* 既定義線種ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFPreColour_checkC		m_PreColour_check;		/* 既定義色ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFText_String_checkC	m_Text_String_check;	/* 文字要素ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFLine_checkC			m_Line_check;			/* 線分ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFCcurveorg_checkC		m_Ccurveorg_check;		/* 複合曲線ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFSfigorg_checkC		m_Sfigorg_check;		/* 複合図形ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFSheet_checkC			m_Sheet_check;			/* 用紙ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFWidth_checkC			m_Width_check;			/* 線幅ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFUsertype_checkC		m_Usertype_check;		/* ﾕｰｻﾞ定義線種ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFUserdefined_color_checkC	m_Usercolor_check;	/* ﾕｰｻﾞ定義色ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
	SXFLayer_checkC			m_Layer_check;			/* ﾚｲﾔﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
//	ADD(S)	Ver3.0対応	K.Naono	02.12.03
	SXFAttribute_checkC		m_Attribute_check;		/* 図面表題欄ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄ */
//	ADD(S)	Ver3.0対応	K.Naono	02.12.03
};

#endif // !defined(AFX_SXFRULECHECKC_H__266DAF61_1152_11D4_A76E_00000EB03267__INCLUDED_)
