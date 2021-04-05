//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFDataStructC.h	                  作成日：2000.03.25
//
//	１．クラス概要
//      Instance DataとHead Section Dataの構造体の定義
//      
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.03.25     周       なし
//
//------------------------------------------------------------
#ifndef _SXFDATASTRUCTC_H_
#define _SXFDATASTRUCTC_H_

#include <afx.h>

struct HEADSECTION{
    CString FileName;
    int     Level;					/* ﾚﾍﾞﾙ */
    int     Mode;					/* ﾓｰﾄﾞ */
										//0:ambilateral(mix)
										//1:Feature
										//2:AP202
	CString Author;					/* 作成者名 */
    CString Organization;			/* 作成者所属 */
    CString Version;				/* ﾊﾞｰｼﾞｮﾝ */
	CString OriginatingSystem;		/* ﾄﾗﾝｽﾚｰﾀ名 */
	int		scadec_flg;				/* SCADECﾌｧｲﾙﾌﾗｸﾞ */
										//-1:SCADEC以外のﾌｧｲﾙ
										//1:SCADECﾌｧｲﾙ
};

#define SXF_IM_NULL         0

#define SXF_IM_COMMENT      1
#define SXF_IM_SECTIONMARK  2
#define SXF_IM_HEADSECTION  3

#define SXF_IM_FT_ENTITY    21
#define SXF_IM_FT_TABLE     22
#define SXF_IM_FT_ASSEMBLY  23


struct INSTANCEDATA{
    int     InstanceMode;//0  NULL,
                         //1  Comment
                         //2  SectionMark
                         //3  HeadSection
                         //21 Feature_Entity
                         //22 Feature_Table
                         //23 Feature_Assembly
	int     InstanceID;		/* ｲﾝｽﾀﾝｽID */
	CString EntityName;		/* ｴﾝﾃｨﾃｨ名 */
    CString EntityData;		/* ｴﾝﾃｨﾃｨﾃﾞｰﾀ */
};

#endif

