/**
	All Rights Reserved,(c) JACIC 2001
	SXFReadMapManagerC.cpp								作成日：2000.4.25

	１．クラス概要
	Read時(フィーチャコメント）のフィーチャオブジェクトと
						Mappingオブジェクトの管理を行うクラス。


	２．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.25			宮園(FQS)			なし。
	更新			2015.04.27			T.Matsushima		JPCERT/CC 指摘 脆弱性対応

**/

#if !defined(AFX_SXFREADMAPMANAGERC_H__7A7CDA80_1A22_11D4_A76E_00000EB03267__INCLUDED_)
#define AFX_SXFREADMAPMANAGERC_H__7A7CDA80_1A22_11D4_A76E_00000EB03267__INCLUDED_

#if _MSC_VER > 1000
#pragma once
#endif // _MSC_VER > 1000

#include "SXFDataStructC.h"

class SXFFeatureC;

class SXFReadMapManagerC  
{
public:
	void SetReturnFlag();
	LPVOID GetAssemblyStructData();
//	LPVOID GetStructData(char*& OUT_TypeName);						JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
	LPVOID GetStructData(char*& OUT_TypeName,size_t TypeNameSize);	// バッファサイズを渡すように変更
	BOOL SetFeatureData(INSTANCEDATA* IN_Instance);
	SXFReadMapManagerC();
	virtual ~SXFReadMapManagerC();

private:
	SXFFeatureC* m_Feature;		/* ﾌｨｰﾁｬｵﾌﾞｼﾞｪｸﾄ */
};

#endif // !defined(AFX_SXFREADMAPMANAGERC_H__7A7CDA80_1A22_11D4_A76E_00000EB03267__INCLUDED_)
