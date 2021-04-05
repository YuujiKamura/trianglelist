//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFInstanceIDC.h                     作成日：2000.04.07
//
//	１．クラス概要
//		インスタンスIDの取得を行う
//      
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.07    周
//
//------------------------------------------------------------
#ifndef _SXFINSTANCEIDC_H_
#define _SXFINSTANCEIDC_H_

#include "SXFFetIDC.h"
#include "SXFDataStructC.h"

class SXFInstanceIDC  
{
public:
	SXFInstanceIDC();
	virtual ~SXFInstanceIDC();
    int GetNewInstanceID();

    SXFFetIDC m_FeatureIDManager;	/* ｲﾝｽﾀﾝｽID管理ｵﾌﾞｼﾞｪｸﾄ */

};

#endif

