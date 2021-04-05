//---------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileWriteC.h	              作成日：2000.03.26
//
//	１．クラス概要
//      ファイルの出力を管理する
//
//	２．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//
//---------------------------------------------------------

#include "SXFDataStructC.h"

class SXFPart21FileC;

class SXFPart21FileWriteC{
public:
//
//	<コンストラクタ>
//
    SXFPart21FileWriteC();
//
//	<デストラクタ>
//
    ~SXFPart21FileWriteC();
	int OpenPart21WriteFile(LPCTSTR IN_FileName );
    int ClosePart21WriteFile(  );
    void SetHeadSection( int  IN_Level,
                         int  IN_Mode,
                         char *IN_FileName,
                         char *IN_Author,
                         char *IN_Organization,
						 char *IN_Version,
                         char *IN_OriginatingSystem );
    int WriteHeadSection( );
    int WriteFeatureInstance(INSTANCEDATA* feature);
protected:
    HEADSECTION			m_HeadSection;		/* ﾍｯﾀﾞ情報 */
    SXFPart21FileC*		m_File;				/* ﾌｧｲﾙ管理ｵﾌﾞｼﾞｪｸﾄ */
};