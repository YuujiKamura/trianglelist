//---------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFPart21FileReadC.h	              作成日：2000.03.26
//
//	１．クラス概要
//		ファイルの入力を管理する
// 
//	２．履歴
//		履歴番号	日付け		 担当者	    概要
//		作成		2000.03.26   周
//
//---------------------------------------------------------
#ifndef _SXFPART21FILEREADC_H_
#define _SXFPART21FILEREADC_H_

class SXFTableManagerC;
class SXFReadFeatureManagerC;
class SXFComInfoC;

class SXFPart21FileReadC{
public:
//
//	<コンストラクタ>
//
    SXFPart21FileReadC();
	SXFPart21FileReadC(SXFTableManagerC* IN_TableManager,
                       SXFReadFeatureManagerC* IN_ReadFeatureManager,
                       SXFComInfoC* IN_ComInfo);

//
//	<デストラクタ>
//
    ~SXFPart21FileReadC();
//
    int OpenPart21ReadFile( LPCTSTR IN_FileName );
    void ClosePart21ReadFile(  );
    void GetHeadData(int *OUT_Level,
                     int *OUT_Mode,
                     char *OUT_FileName,
                     char *OUT_Author,
                     char *OUT_Organization,
                     char *OUT_Version,
                     char *OUT_OriginatingSystem );
protected:
    BOOL RegisterFeatureEntity(INSTANCEDATA *IN_Instance);
    BOOL RegisterFeatureTable(INSTANCEDATA *IN_Instance);
    BOOL RegisterFeatureAssembly(INSTANCEDATA *IN_Instance);
    void ClearInstanceData();

	SXFTableManagerC*	m_TableManager;	/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ */
	SXFReadFeatureManagerC* m_ReadFeatureManager;	/* Read管理ｵﾌﾞｼﾞｪｸﾄ */
    HEADSECTION				m_HeadSection;			/* ﾍｯﾀﾞ情報 */
    INSTANCEDATA			m_InstanceData;			/* ｲﾝｽﾀﾝｽﾃﾞｰﾀ */
    SXFPart21FileC*			m_File;					/* ﾌｧｲﾙ管理ｵﾌﾞｼﾞｪｸﾄ */
	SXFComInfoC*			m_ComInfo;				/* 共通情報管理ｵﾌﾞｼﾞｪｸﾄ */
};

#endif

