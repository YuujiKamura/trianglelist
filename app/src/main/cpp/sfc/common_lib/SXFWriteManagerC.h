//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFWriteManagerC.h                    作成日：2000.04.18
//
//	１．クラス概要
//      出力時のアセンブリ間の参照関係を管理する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.18    周
//
//------------------------------------------------------------
#ifndef _SXFWRITEMANAGERC_H_
#define _SXFWRITEMANAGERC_H_

#include <afx.h>
#include <afxtempl.h>

typedef struct sfigorgcheck{
    int type;			//複合図形種別
							//1,2:部分図
							//3:作図グループ
							//4:作図部品
    CString name;		//複合図形名
	int locate_flag ;	//未配置:-1 配置済:1
	int error;			//定義がエラーのときは-1
	sfigorgcheck() {
		type		= -1 ;
		locate_flag	= -1 ;
		error		= -1 ;
	}
}SXFSFIGORGCHECK;

//typedef CArray<SXFSFIGORGCHECK,SXFSFIGORGCHECK&> SXFSFIGORGTABLE;
typedef CList<SXFSFIGORGCHECK*,SXFSFIGORGCHECK*&> SXFSFIGORGTABLE;
#include "SXFWriteMappingManagerC.h"

class SXFWriteMappingManagerC;
class SXFPart21FileWriteC;
class SXFInstanceIDC;

class SXFWriteManagerC  
{
public:
	SXFWriteManagerC();
	SXFWriteManagerC(SXFPart21FileWriteC* IN_WriteFile,
					SXFInstanceIDC* IN_InstanceIDManager);
	virtual ~SXFWriteManagerC();
	void ErrorAssembly();
	int GetAssemType();
	int CloseAssembly();
    int RegisterAssembly(int IN_AssemblyType,LPVOID IN_Struct);
    int RegisterFeature(char* IN_FeatureType,LPVOID IN_Struct);
	BOOL SfigNameCheck( LPCTSTR IN_Name);
	BOOL GetSfigFlg( LPCTSTR IN_Name,
					int* OUT_Type,
					int* OUT_LocFlg);
	BOOL SetLocFlg( LPCTSTR IN_Name);
	BOOL CheckHatchID( unsigned int IN_ID);

private:
    CUIntArray					m_CurveTable;			/** 複合曲線ﾃｰﾌﾞﾙ **/
	int							m_CurveFlag;			/** 複合曲線ﾌﾗｸﾞ **/
	SXFPart21FileWriteC*		m_WriteFile;			/** ﾌｧｲﾙ出力ｵﾌﾞｼﾞｪｸﾄ **/
	SXFInstanceIDC*				m_InstanceIDManager;	/** ｲﾝｽﾀﾝｽIDｵﾌﾞｼﾞｪｸﾄ **/
	int							m_SfigTableIndex;		/** 複合図形ﾃｰﾌﾞﾙﾝﾃﾞｯｸｽ **/
	int							m_SfigFlag;				/** 複合図形ﾌﾗｸﾞ **/
	int							m_AssemType;			/** ｱｾﾝﾌﾞﾘ型 **/
	BOOL						m_SheetAssemblyFlag;	/** 用紙ﾌﾗｸﾞ **/
    SXFWriteMappingManagerC*	m_CurrentAssembly;		/** ｱｾﾝﾌﾞﾘ用Mapping管理ｵﾌﾞｼﾞｪｸﾄ **/
    SXFWriteMappingManagerC*	m_CurrentChild;			/** 一般要素用Mapping管理ｵﾌﾞｼﾞｪｸﾄ **/
    CUIntArray*					m_ChildrenArray;		/** 一般要素リスト **/
    BOOL						m_FirstAssemblyFlag;	/** ｱｾﾝﾌﾞﾘﾌﾗｸﾞ **/
	int							m_AssemblyCode;			/** ｱｾﾝﾌﾞﾘｺｰﾄﾞ **/
	SXFSFIGORGTABLE				m_SfigorgTable;			/** 複合図形ﾃｰﾌﾞﾙ **/
	BOOL						m_ErrorFlag;			/** ｱｾﾝﾌﾞﾘｴﾗｰﾌﾗｸﾞ **/
	CMapStringToPtr				m_mapSfigOrg ;
};

#endif
