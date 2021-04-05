//---------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileC.h	                  作成日：2000.03.25
//
//	１．クラス概要
//      ファイルの入力と出力を管理する
//
//	２．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.25  周
//		更新		2002.02.20	K.Naono		ヘッダ解析処理の改善対応
//
//---------------------------------------------------------
#include "SXFDataStructC.h"

class SXFPart21FileC:public CStdioFile{

public:
//
//	<コンストラクタ>
//
    SXFPart21FileC();
    SXFPart21FileC( LPCTSTR IN_lpszFileName, UINT IN_nOpenFlags);

//
//	<デストラクタ>
//
    ~SXFPart21FileC();

//	MOD(S)	ヘッダ解析処理の改善対応	K.Naono		02.02.20
//	int  ReadHeadSection(HEADSECTION *OUT_Head);
	virtual int  ReadHeadSection(HEADSECTION* IO_Head);
//	MOD(E)	ヘッダ解析処理の改善対応	K.Naono		02.02.20

	int	 ReadHeaderInstance(INSTANCEDATA *OUT_Instance);
    int  ReadInstance(INSTANCEDATA *OUT_Instance);
    void WriteHeadSection(HEADSECTION *IN_Head);
    void WriteInstance(INSTANCEDATA *IN_Instance);

private:
	int featurecount;			/* ﾌｨｰﾁｬｲﾝｽﾀﾝｽ数 */

};
