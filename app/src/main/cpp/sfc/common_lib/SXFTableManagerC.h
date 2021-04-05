//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.h                    作成日：2000.04.10
// 
//	１．クラス概要
//      定義テーブルデータ値と定義テーブルMapping管理
//      オブジェクトのアドレスを保持し、定義テーブル
//      Mapping管理オブジェクトを管理する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
#ifndef _SXFTABLEMANAGERC_H_
#define _SXFTABLEMANAGERC_H_

#include <afxtempl.h>

#include "SXFLayerC.h"
#include "SXFPreDefFontC.h"
#include "SXFUserDefFontC.h"
#include "SXFPreDefColorC.h"
#include "SXFUserDefColorC.h"
#include "SXFWidthC.h"
#include "SXFTextFontC.h"
#include "SXFDataStructC.h"
#include "SXFComFunctionC.h"

class SXFComInfoC;
class SXFPart21FileWriteC;
class SXFInstanceIDC;

class SXFTableManagerC  : public SXFComFunctionC
{
public:
	SXFTableManagerC();
	SXFTableManagerC(SXFComInfoC* IN_CommonInfo);
	SXFTableManagerC(SXFComInfoC* IN_CommonInfo,
						SXFPart21FileWriteC* IN_WriteFile,
						SXFInstanceIDC* IN_InstanceIDManager);
	SXFTableManagerC(SXFComInfoC* IN_CommonInfo,SXFInstanceIDC* IN_InstanceIDManager);
	virtual ~SXFTableManagerC();
	int RegisterStructTable( int IN_Type, LPVOID IN_Struct);
    int GetID(int IN_Type, int IN_Code);
    BOOL CheckSameLayer( LPCTSTR IN_Name);
    BOOL CheckSamePreDefColor( LPCTSTR IN_Name);
    BOOL CheckSamePreDefFont( LPCTSTR IN_Name);
    BOOL CheckSameUserDefFont( LPCTSTR IN_Name);
    BOOL CheckSameWidth( double IN_Width);
    BOOL CheckSameTextFont( LPCTSTR IN_Name);
    void LayerMapping();
	BOOL RegisterFeatureTable( INSTANCEDATA *IN_Instance );
 	BOOL GetTableElement(int IN_Type, LPVOID OUT_Struct, int* OUT_EndFlg);

private:
    CArray<SXFLayerC,SXFLayerC&>                m_LayerTable;				/* ﾚｲﾔﾃｰﾌﾞﾙ */
    SXFPreDefFontC                              m_PreDefFontTable[16];		/* 既定義線種ﾃｰﾌﾞﾙ */
    CArray<SXFUserDefFontC, SXFUserDefFontC&>   m_UserDefFontTable;			/* ﾕｰｻﾞ定義線種ﾃｰﾌﾞﾙ */
    SXFPreDefColorC                             m_PreDefColorTable[16];		/* 既定義色ﾃｰﾌﾞﾙ */
    CArray<SXFUserDefColorC, SXFUserDefColorC&> m_UserDefColorTable;		/* ﾕｰｻﾞ定義色ﾃｰﾌﾞﾙ */
    SXFWidthC                                   m_WidthTable[16];			/* 線幅ﾃｰﾌﾞﾙ */
	CArray<SXFTextFontC, SXFTextFontC&>         m_TextFontTable;			/* 文字ﾌｫﾝﾄﾃｰﾌﾞﾙ */
	SXFPart21FileWriteC*						m_WriteFile;				/* ﾌｧｲﾙ出力ｵﾌﾞｼﾞｪｸﾄ */
    SXFComInfoC*								m_ComInfo;					/* 共通情報管理ｵﾌﾞｼﾞｪｸﾄ */
    SXFInstanceIDC*								m_InstanceIDManager;		/* ｲﾝｽﾀﾝｽIDｵﾌﾞｼﾞｪｸﾄ */
	BOOL										m_LayerFirstFlag;			/* ﾚｲﾔﾌﾗｸﾞ */
    int											m_CurrentType;				/* 現在処理中のﾃｰﾌﾞﾙ型 */
    int											m_CurrentNumber;			/* 現在処理中のﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽ */
    int											m_PreDefWidthFlagTable[10];	/* 線幅ﾌﾗｸﾞﾃｰﾌﾞﾙ */
	int											userdefwidthcode;			/* 線幅ｺｰﾄﾞ */
};

#endif
