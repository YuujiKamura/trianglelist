/**
	All Rights Reserved,(c) JACIC 2001
	SXFTableMapManagerC.h								作成日：2000.4.14

	１．クラス概要
	定義テーブルフィーチャオブジェクトとMappingオブジェクトを管理するクラス

	２．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.14			宮園(FQS)			なし。

**/

#if !defined(AFX_SXFTABLEMAPMANAGERC_H__16B30B22_0BAC_11D4_A76E_00000EB03267__INCLUDED_)
#define AFX_SXFTABLEMAPMANAGERC_H__16B30B22_0BAC_11D4_A76E_00000EB03267__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include <afx.h>
#include "SXFDataStructC.h"
#include "SXFStruct.h"

class SXFTableFeatureC;
class SXFPreDefColorC;
class SXFLayerC;
class SXFPart21FileWriteC;
class SXFInstanceIDC;
class SXFTableManagerC;
class SXFTableMapManagerC  
{
public:
    SXFTableMapManagerC();
	SXFTableMapManagerC(SXFPart21FileWriteC* IN_WriteFile,
						SXFInstanceIDC* m_InstanceIDManager);    
    virtual ~SXFTableMapManagerC();
	//WRITE
	void SetStructData(int IN_type,LPVOID IN_Struct);
	int WriteInstance(int* OUT_InstanceID);
	BOOL SetStructLayer(SXFLayerC* IN_Layer,int* OUT_InstanceID);
	//READ
	LPVOID GetStructData();
	void SetFeatureData(int IN_Type,LPVOID IN_Class);
	void SetStructPrecolor(int IN_Type,
							LPVOID IN_Struct,
							SXFPreDefColorC IN_Precolor);

private:
	SXFTableFeatureC*		m_LayerFeature;			/* 定義ﾃｰﾌﾞﾙﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ */
	SXFInstanceIDC*			m_InstanceIDManager;	/* ｲﾝｽﾀﾝｽIDｵﾌﾞｼﾞｪｸﾄ */
	SXFPart21FileWriteC*	m_WriteFile;			/* ﾌｧｲﾙ出力ｵﾌﾞｼﾞｪｸﾄ */
	SXFTableFeatureC*		m_TableFeature;			/* 定義ﾃｰﾌﾞﾙﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ */
};

#endif // !defined(AFX_SXFTABLEMAPMANAGERC_H__16B30B22_0BAC_11D4_A76E_00000EB03267__INCLUDED_)
