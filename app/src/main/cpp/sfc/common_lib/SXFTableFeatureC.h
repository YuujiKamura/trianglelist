//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableFeatureC.h                    作成日：2000.04.10
//
//	１．クラス概要
//		定義テーブルフィーチャデータを保持する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
#ifndef _SXFTABLEFEATUREC_H_
#define _SXFTABLEFEATUREC_H_

class SXFInstanceIDC;
class SXFLayerC;

class SXFTableFeatureC  
{
public:
	SXFTableFeatureC();
	SXFTableFeatureC(SXFInstanceIDC* IN_InstanceIDManager);
	virtual ~SXFTableFeatureC();
	//WRITE
	void SetLayerFeature(SXFLayerC* IN_Layer);
    void SetFeatureData(int IN_Type,LPVOID IN_Struct);
    INSTANCEDATA* GetFeatureData( );
	//READ
    void SetStructData(int IN_Type, LPVOID IN_Class);
    LPVOID GetStructData();

private:
    INSTANCEDATA	m_FeatureData;			/* 定義ﾃｰﾌﾞﾙ要素ｲﾝｽﾀﾝｽﾃﾞｰﾀ */
    LPVOID			m_StructData;			/* 定義ﾃｰﾌﾞﾙ構造体 */
	SXFInstanceIDC* m_InstanceIDManager;	/* ｲﾝｽﾀﾝｽIDｵﾌﾞｼﾞｪｸﾄ */

};

#endif
