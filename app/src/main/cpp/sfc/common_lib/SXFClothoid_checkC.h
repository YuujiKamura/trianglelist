/**
	All Rights Reserved,(c) JACIC 2001
	SXFClothoid_checkC.h								作成日：2007.1.29

	１．クラス概要
	クロソイドフィーチャのルールチェックを行うクラス

	２．履歴
	履歴番号		日付け				担当者				概要
	作成			2007.1.29			西木				なし。

**/

#if !defined(AFX_SXFCLOTHOID_CHECKC_H__72D65A23_0949_11D4_A76E_00000EB03267__INCLUDED_)
#define AFX_SXFCLOTHOID_CHECKC_H__72D65A23_0949_11D4_A76E_00000EB03267__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include <afx.h>
#include "SXFStruct.h"
#include "SXFComFunctionC.h"
#include "SXFTableCheckC.h"

class SXFTableManagerC;
class SXFComInfoC;

class SXFClothoid_checkC   : virtual public SXFTableCheckC , public SXFComFunctionC
{
public:
	int clothoidstr_rule_check(Clothoid_Struct* IN_clothoid_str);
	int attr_rule_check(Clothoid_Struct* IN_clothoid_str);
	int Feature_rule_check(Clothoid_Struct* IN_clothoid_str,
							SXFTableManagerC* IN_TableManager,
							CString IN_InstanceID,
							SXFComInfoC* IN_Info);
	SXFClothoid_checkC();
	virtual ~SXFClothoid_checkC();
};

#endif // !defined(AFX_SXFCLOTHOID_CHECKC_H__72D65A23_0949_11D4_A76E_00000EB03267__INCLUDED_)
