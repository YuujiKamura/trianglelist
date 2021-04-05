/**
	All Rights Reserved,(c) JACIC 2001
	SXFWriteMappingManagerC.h								作成日：2000.4.14

	１．クラス概要
	Write時のフィーチャオブジェクトとMappingオブジェクトの管理を行うクラス

	２．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/

#if !defined(AFX_SXFWRITEMAPPINGMANAGERC_H__A623AA20_11F2_11D4_A76E_00000EB03267__INCLUDED_)
#define AFX_SXFWRITEMAPPINGMANAGERC_H__A623AA20_11F2_11D4_A76E_00000EB03267__INCLUDED_

#include "SXFStruct.h"	// ClassView によって追加されました。
#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include <afx.h>
#include "SXFStruct.h"
#include "SXFWriteManagerC.h"

class SXFPart21FileWriteC;
class SXFInstanceIDC;
class SXFFeatureC;

class SXFWriteMappingManagerC  
{
public:
	int MappingFeature(char *IN_FeatureType, LPVOID IN_Struct,int &OUT_Id1);
	int MappingAssembly();
	void SetStructData(int IN_Asem_Type,LPVOID IN_Struct);
	SXFWriteMappingManagerC();
	SXFWriteMappingManagerC(int IN_AssemType,
							SXFPart21FileWriteC* IN_WriteFile,
							SXFInstanceIDC* IN_InstanceIDManager);
	void ClearData();
	virtual ~SXFWriteMappingManagerC();

private:
	SXFFeatureC*			m_AsemFeature;			/* ｱｾﾝﾌﾞﾘﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ */
	SXFFeatureC*			m_Feature;				/* 一般要素ﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ */
	SXFPart21FileWriteC*	m_WriteFile;			/* ﾌｧｲﾙ出力ｵﾌﾞｼﾞｪｸﾄ */
	SXFInstanceIDC*			m_InstanceIDManager;	/* ｲﾝｽﾀﾝｽIDｵﾌﾞｼﾞｪｸﾄ */
	int						m_AssemType;			/* ｱｾﾝﾌﾞﾘ型 */
	Sheet_Struct			m_Sheet_Str;			/* 用紙構造体 */
	Sfigorg_Struct			m_Sfigorg_Str;			/* 複合図形定義構造体 */
	Ccurve_Org_Struct		m_CompCurve_Str;		/* 複合曲線定義 */
};

#endif // !defined(AFX_SXFWRITEMAPPINGMANAGERC_H__A623AA20_11F2_11D4_A76E_00000EB03267__INCLUDED_)
