//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.h                         作成日：2000.04.17
//
//	１．クラス概要
//      一般要素フィーチャーデータを保持する     
//
//	２．履歴
//	    履歴番号	日付け			担当者			概要
//      作成		2000.04.17		周
//		更新		2015.04.27		T.Matsushima	JPCERT/CC 指摘 脆弱性対応
//
//------------------------------------------------------------
#ifndef _SXFFEATUREC_H_
#define _SXFFEATUREC_H_

#include <afx.h>
#include "SXFDataStructC.h"

class SXFInstanceIDC;

class SXFFeatureC  
{
public:
	void SetReturnFlag();
	SXFFeatureC();
	SXFFeatureC(SXFInstanceIDC* IN_InstanceIDManager);
	virtual ~SXFFeatureC();
	//read
    BOOL SetStructData(INSTANCEDATA* IN_Feature);
	LPVOID GetAssemblyStructData();
//	LPVOID GetStructData(char*& OUT_Type);					JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
	LPVOID GetStructData(char*& OUT_Type,size_t TypeSize);	// バッファサイズを渡すように変更
	//write
    void SetFeatureData(LPCTSTR IN_TypeName,LPVOID IN_Struct);
	void SetAssemblyData(int IN_Type,LPVOID IN_Struct);
    INSTANCEDATA* GetFeatureData( );
	int GetSheetX(int IN_Type);
	int GetSheetY(int IN_Type);
private:
	int				m_ReturnFlag;			/* ﾌｨｰﾁｬ構造体返却済みﾌﾗｸﾞ */
    INSTANCEDATA	m_FeatureData;			/* 一般要素ﾌｨｰﾁｬｲﾝｽﾀﾝｽﾃﾞｰﾀ */
    char			m_TypeName[30];			/* ﾌｨｰﾁｬ型 */
    LPVOID			m_StructData;			/* 一般要素構造体 */
	SXFInstanceIDC* m_InstanceIDManager;	/* ｲﾝｽﾀﾝｽID管理ｵﾌﾞｼﾞｪｸﾄ */
};

#endif

