//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFReadFeatureManagerC.h               作成日：2000.04.23
//
//	１．クラス概要
//      FeatureCommentアセンブリ要素、
//      一般要素の親子関係を管理する
//
//	２．履歴
//		履歴番号	日付け			担当者			 概要
//		作成		2000.04.23		周
//		更新		2015.04.27		T.Matsushima	JPCERT/CC 指摘 脆弱性対応
//
//------------------------------------------------------------
#ifndef _SXFFEATURECHILDC_H_
#define _SXFFEATURECHILDC_H_

#include <afx.h>
#include <afxtempl.h>
#include "SXFFeatureAssemblyC.h"
#include "SXFDataStructC.h"

class SXFReadMapManagerC;
class SXFFeatureChildC;

typedef CTypedPtrList<CPtrList,SXFFeatureAssemblyC*> SXFASSEMBLYLIST;

class SXFReadFeatureManagerC  
{
public:
	SXFReadFeatureManagerC();
	virtual ~SXFReadFeatureManagerC();
	BOOL RegisterFeatureAssembly(INSTANCEDATA *IN_Instance);
	BOOL RegisterFeatureEntity(INSTANCEDATA *IN_Instance);
//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
	int GetNextFeature(char* OUT_TypeName,
							size_t	TypeNameSize,	// バッファサイズを渡すように変更
							LPVOID& OUT_Struct,
							int* OUT_EndFlg);
	int GetNextAssembly(int IN_Type, 
							LPVOID& OUT_Struct, 
							int* OUT_EndFlg,
							int& OUT_InstanceID);
	BOOL GetSfigFlg( LPCTSTR IN_Name,int* OUT_SfigFlg,int*OUT_LocFlg);
	BOOL SetLocFlg( LPCTSTR IN_Name);
	BOOL CheckHatchID( int IN_ID);
	void Delete_m_CurrentChildList();

private:
    SXFASSEMBLYLIST*     m_CurveAssemblyList;	/* 複合曲線ﾌｨｰﾁｬﾃｰﾌﾞﾙ */
	SXFASSEMBLYLIST*     m_SymbolAssemblyList;	/* 複合図形ﾌｨｰﾁｬﾃｰﾌﾞﾙ */
	CMapStringToPtr		 m_SymbolInfoTable;		// 2010.08
    SXFFeatureAssemblyC* m_SheetAssembly;		/* 用紙ﾌｨｰﾁｬのｵﾌﾞｼﾞｪｸﾄ */
    SXFASSEMBLYLIST*     m_AssemblyList;		/* ｱｾﾝﾌﾞﾘﾃｰﾌﾞﾙ */
    SXFFeatureAssemblyC* m_CurrentAssembly;		/* 現在処理中のｱｾﾝﾌﾞﾘ要素ｵﾌﾞｼﾞｪｸﾄ */
    SXFCHILDLIST*        m_CurrentChildList;	/* 現在処理中の一般要素ﾃｰﾌﾞﾙ */
    SXFFeatureChildC*    m_CurrentChild;		/* 現在処理中の一般要素ｵﾌﾞｼﾞｪｸﾄ */
    SXFReadMapManagerC*  m_CurrentMapManager;	/* 現在処理中のMapping管理ｵﾌﾞｼﾞｪｸﾄ */
    POSITION			 m_AssemblyPos;			/* ｱｾﾝﾌﾞﾘﾃｰﾌﾞﾙのPosition */
    POSITION			 m_ChildPos;			/* 一般要素ﾃｰﾌﾞﾙのPosition */
	int					 m_CurrentAssemblyType;	/* 現在処理中のｱｾﾝﾌﾞﾘﾀｲﾌﾟ */
	int					 m_code;				/* ｱｾﾝﾌﾞﾘｺｰﾄﾞ */
};

#endif
