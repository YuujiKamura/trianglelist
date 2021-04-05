//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFInstanceIDC.cpp                    作成日：2000.04.07
//
//	１．クラス概要
//		インスタンスIDの取得を行う
//      
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.07    周
//
//------------------------------------------------------------
#include "stdafx.h"
#include "SXFDataStructC.h"
#include "SXFInstanceIDC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFInstanceIDC.cpp                    作成日：2000.04.07
//
//  １．機能概要
//      クラス構築
//
//  ２．インターフェース
//		void	SXFInstanceIDC::SXFInstanceIDC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.07    周
//
//------------------------------------------------------------
SXFInstanceIDC::SXFInstanceIDC()
{

}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFInstanceIDC.cpp                    作成日：2000.04.07
//
//  １．機能概要
//      クラス消滅
//  ２．インターフェース
//		void	SXFInstanceIDC::~SXFInstanceIDC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.07    周
//
//------------------------------------------------------------
SXFInstanceIDC::~SXFInstanceIDC()
{

}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFInstanceIDC.cpp                    作成日：2000.04.07
//
//  １．機能概要
//      新しいInstance IDを返す
//  ２．インターフェース
//		int SXFInstanceIDC::GetNewInstanceID()
//
//	引数	：	型		引数名				内容
//		なし
//
//	    復帰値	
//            int	A New Entity ID
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.07    周
//
//------------------------------------------------------------

int SXFInstanceIDC::GetNewInstanceID()
{
    int NewID=-1;
    NewID = m_FeatureIDManager.Get_ID();
    return NewID;
}
