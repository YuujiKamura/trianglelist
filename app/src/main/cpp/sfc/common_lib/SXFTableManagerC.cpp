//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
// 
//	１．クラス概要
//		定義テーブルデータ値と定義テーブルMapping管理
//		オブジェクトのアドレスを保持し、定義テーブル
//		Mapping管理オブジェクトを管理する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
#include "stdafx.h"
#include <math.h>
#include "SXFDataStructC.h"
#include "SXFPreWidthTableC.h"
#include "SXFTableManagerC.h"
#include "SXFStruct.h"
#include "SXFTableMapManagerC.h"
#include "SXFErrorMsgC.h"
#include "SXF_EXT.h"
#include "ComSplitStringC.h"
#include "SXFComInfoC.h"
#include "SXFPart21FileWriteC.h"
#include "SXFInstanceIDC.h"
#include "SXFSplitFeatureC.h"
#include "SXFRGBTableC.h"
#include "SXFPreLineTypeTableC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      クラス構築
//
//  ２．インターフェース
//		void	SXFTableManagerC::SXFTableManagerC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
SXFTableManagerC::SXFTableManagerC()
{	
	m_LayerFirstFlag = TRUE;
    m_CurrentType=0;
    m_CurrentNumber=0;
    for(int i=0;i<10;i++)
        m_PreDefWidthFlagTable[i]=0;
	userdefwidthcode=11;		/* ﾕｰｻﾞ定義線幅ｺｰﾄﾞは11～ */
	m_WriteFile= NULL;
    m_ComInfo=NULL;
    m_InstanceIDManager=NULL;

}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      クラス構築
//
//  ２．インターフェース
//	void SXFTableManagerC::SXFTableManagerC(SXFComInfoC* IN_CommonInfo)
//		
//	引数 ：	型				引数名			内容
//			SXFComInfoC*	IN_CommonInfo	共通情報ｵﾌﾞｼﾞｪｸﾄ
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
SXFTableManagerC::SXFTableManagerC(SXFComInfoC* IN_CommonInfo)
{	
	m_ComInfo = IN_CommonInfo;
	m_LayerFirstFlag = TRUE;
    m_CurrentType=0;
    m_CurrentNumber=0;
    for(int i=0;i<10;i++)
        m_PreDefWidthFlagTable[i]=0;
	m_WriteFile= NULL;
    m_InstanceIDManager=NULL;
	userdefwidthcode=11;		/* ﾕｰｻﾞ定義線幅ｺｰﾄﾞは11～ */
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      クラス構築
//
//  ２．インターフェース
//	void SXFTableManagerC::SXFTableManagerC(
//								SXFComInfoC* IN_CommonInfo,
//				                SXFPart21FileWriteC* IN_WriteFile,
//							    SXFInstanceIDC* IN_InstanceIDManager)
//
//		
//	引数 ：	型					  引数名				内容
//			SXFComInfoC*		  IN_CommonInfo			共通情報ｵﾌﾞｼﾞｪｸﾄのｱﾄﾞﾚｽ
//          SXFPart21FileWriteC*  IN_WriteFile			ﾌｧｲﾙ出力ｵﾌﾞｼﾞｪｸﾄのｱﾄﾞﾚｽ
//          SXFInstanceIDC*		  IN_InstanceIDManager	ｲﾝｽﾀﾝｽIDｵﾌﾞｼﾞｪｸﾄのｱﾄﾞﾚｽ
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
SXFTableManagerC::SXFTableManagerC(SXFComInfoC* IN_CommonInfo,
                     SXFPart21FileWriteC* IN_WriteFile,
                     SXFInstanceIDC* IN_InstanceIDManager)
{
	m_WriteFile= IN_WriteFile;
    m_ComInfo=IN_CommonInfo;
    m_InstanceIDManager=IN_InstanceIDManager;
	m_LayerFirstFlag = TRUE;
    m_CurrentType=0;
    m_CurrentNumber=0;
    for(int i=0;i<10;i++)
        m_PreDefWidthFlagTable[i]=0;
	userdefwidthcode=11;		/* ﾕｰｻﾞ定義線幅ｺｰﾄﾞは11～ */
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      クラス消滅
//
//  ２．インターフェース
//		void	SXFTableManagerC::~SXFTableManagerC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
SXFTableManagerC::~SXFTableManagerC()
{
    //delete mapping class in layer table
    for(int i=0;i<m_LayerTable.GetSize();i++){
        if(m_LayerTable[i].m_MappingManager!=NULL)
            delete m_LayerTable[i].m_MappingManager;
    }
    //delete mapping class in predefcolor table
    for( i=0;i<16;i++){
        if(m_PreDefColorTable[i].m_MappingManager!=NULL)
            delete m_PreDefColorTable[i].m_MappingManager;
    }
    //delete mapping class in userdefcolor table
    for( i=0;i<m_UserDefColorTable.GetSize();i++){
        if(m_UserDefColorTable[i].m_MappingManager!=NULL)
            delete m_UserDefColorTable[i].m_MappingManager;
    }
    //delete mapping class in predeffont table
    for( i=0;i<16;i++){
        if(m_PreDefFontTable[i].m_MappingManager!=NULL)
            delete m_PreDefFontTable[i].m_MappingManager;
    }
    //delete mapping class in userdeffont table
    for( i=0;i<m_UserDefFontTable.GetSize();i++){
        if(m_UserDefFontTable[i].m_MappingManager!=NULL)
            delete m_UserDefFontTable[i].m_MappingManager;
    }
    //delete mapping class in width table
    for(i=0;i<16;i++){
        if(m_WidthTable[i].m_MappingManager!=NULL)
            delete m_WidthTable[i].m_MappingManager;
    }
    //delete mapping class in textfont table
    for(i=0;i<m_TextFontTable.GetSize();i++){
        if(m_TextFontTable[i].m_MappingManager!=NULL)
            delete m_TextFontTable[i].m_MappingManager;
    }
}


//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      Feature Comment Table Instanceを登録する(READ)
//      
//  ２．インターフェース
//      BOOL RegisterFeatureTable( INSTANCEDATA *IN_Instance )
//
//	    引数: 型			  引数名		    内容
//            INSTANCEDATA*   IN_Instance       Instanceの
//                                              構造体のアドレス 
//      
//	    復帰値	
//            正常時：TRUE
//			  異常時：FALSE
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
BOOL SXFTableManagerC::RegisterFeatureTable(INSTANCEDATA *IN_Instance )
{
	/** レイヤ **/
    if(IN_Instance->EntityName == "layer_feature"){
        SXFLayerC layer;
		/* ﾚｲﾔｺｰﾄﾞ */
        INT_PTR layercode = m_LayerTable.GetSize()+1;
		/** 最大レイヤ数を超えたときエラー **/
        if (layercode > MAXLAYER){// max layer code = 256 
			SXFErrorMsgC::RecordError(SXFMAXLAYER,"LAYER",0);
			return FALSE;
		}
		/* ﾚｲﾔｺｰﾄﾞ設定 */
        layer.m_Code = (int)layercode;
        CStringArray* array;
        SXFSplitFeatureC split(IN_Instance->EntityData);
        array = split.Split();
        if(array == NULL)
            return FALSE;
        if (array->GetSize() != 2) // syntax err
            return FALSE;
		/* ﾚｲﾔ名 */
        layer.m_Name = (* array)[0];
		//レイヤ名が指定されていないとき
		if (strlen(layer.m_Name) == 0){
			SXFErrorMsgC::RecordError(SXFNOTSETLNAME,"LAYER",0);
			return FALSE;
		}
		/* ﾚｲﾔ名が既にﾃｰﾌﾞﾙ内にあればｴﾗｰ */
        if (!CheckSameLayer(layer.m_Name)){
			SXFErrorMsgC::RecordError(SXFSAMENAME,"LAYER",1,layer.m_Name);
			return FALSE;
		}
		/* 表示／非表示ﾌﾗｸﾞ */
        int lfflg = atoi( (* array)[1] );
        layer.m_LfFlag = lfflg;
		//表示／非表示フラグのチェック
		if (layer.m_LfFlag < 0 || layer.m_LfFlag > 1) {
			SXFErrorMsgC::RecordError(SXFLAYERFLAG,"LAYER",1,layer.m_LfFlag);
			return FALSE;
		}
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
        SXFTableMapManagerC* mappingmanager = new SXFTableMapManagerC(); 
		/* ﾚｲﾔ情報ｾｯﾄ */
        mappingmanager->SetFeatureData( 1 ,&layer );//class type && ptr of the class
		/* ｲﾝｽﾀﾝｽIDｾｯﾄ */
        layer.m_ID = IN_Instance->InstanceID;
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
        layer.m_MappingManager = mappingmanager;
		/* ﾚｲﾔﾃｰﾌﾞﾙに追加 */
        m_LayerTable.Add(layer);
    }
	/** 既定義色 **/
	else if(IN_Instance->EntityName == "pre_defined_colour_feature"){
        SXFPreDefColorC predefcolor;
        CStringArray* array;
        SXFSplitFeatureC split(IN_Instance->EntityData);
        array = split.Split();
        if(array == NULL)
            return FALSE;
        if (array->GetSize() != 1) // syntax err
            return FALSE;
		/* 既定義色以外の名前のとき */
        if (!predefcolor.SetName((* array)[0])){
			SXFErrorMsgC::RecordError(SXFNOTPRECOLOUR,"PREDEFINED_COLOUR",1,(* array)[0]);
			return FALSE;
		}
		/* 色名が既にﾃｰﾌﾞﾙ内にあるとき */
		if (!CheckSamePreDefColor((* array)[0])){
			SXFErrorMsgC::RecordError(SXFSAMENAME,"PREDEFINED_COLOUR",1,(* array)[0]);
			return FALSE;
		}
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
        SXFTableMapManagerC* mappingmanager = new SXFTableMapManagerC(); 
		/* 色情報をｾｯﾄ */
        mappingmanager->SetFeatureData(2,&predefcolor);
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄをｾｯﾄ */
        predefcolor.m_MappingManager=mappingmanager;
		/* ｲﾝｽﾀﾝｽIDをｾｯﾄ */
        predefcolor.m_ID = IN_Instance->InstanceID;
		/* 既定義色ﾃｰﾌﾞﾙに追加 */
        m_PreDefColorTable[predefcolor.m_Code-1] = predefcolor;
    }
	/** ユーザ定義色 **/
	else if(IN_Instance->EntityName == "user_defined_colour_feature"){
        SXFUserDefColorC userdefcolor;
        userdefcolor.m_Code = (int) m_UserDefColorTable.GetSize()+17;
		/** 最大色数を超えたときエラー処理 **/
        if(userdefcolor.m_Code > MAXCOLOUR){
			SXFErrorMsgC::RecordError(SXFMAXUSERCOLOUR,"USERDEFINED_COLOUR",0);
			return FALSE;
		}
        CStringArray* array;
        SXFSplitFeatureC split(IN_Instance->EntityData);
        array = split.Split();
        if(array == NULL)
            return FALSE;
        if (array->GetSize() != 3) // syntax err
            return FALSE;
        userdefcolor.R=atoi( (* array)[0] );
        userdefcolor.G=atoi( (* array)[1] );
        userdefcolor.B=atoi( (* array)[2] );
		//R値が0から255でないとき
		if ((userdefcolor.R < 0) || (userdefcolor.R > MAXRGB)){
			SXFErrorMsgC::RecordError(SXFRGBERROR,
										"USERDEFINED_COLOUR",
										1,
										userdefcolor.R);
			return FALSE;
		}
		//G値が0から255でないとき
		if ((userdefcolor.G < 0) || (userdefcolor.G > MAXRGB)){
			SXFErrorMsgC::RecordError(SXFRGBERROR,
										"USERDEFINED_COLOUR",
										1,
										userdefcolor.G);
			return FALSE;
		}
		//B値が0から255でないとき
		if ((userdefcolor.B < 0) || (userdefcolor.B > MAXRGB)){
			SXFErrorMsgC::RecordError(SXFRGBERROR,
										"USERDEFINED_COLOUR",
										1,
										userdefcolor.B);
			return FALSE;
		}
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
        SXFTableMapManagerC* mappingmanager = new SXFTableMapManagerC(); 
		/* ﾕｰｻﾞ定義色情報ｾｯﾄ */
        mappingmanager->SetFeatureData(3,&userdefcolor);
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
        userdefcolor.m_MappingManager=mappingmanager;
		/* ｲﾝｽﾀﾝｽIDｾｯﾄ */
        userdefcolor.m_ID = IN_Instance->InstanceID;
		/* ﾕｰｻﾞ定義色ﾃｰﾌﾞﾙに追加 */
        m_UserDefColorTable.Add(userdefcolor);
    }
	/** 既定義線種 **/
	else if(IN_Instance->EntityName == "pre_defined_font_feature"){
        SXFPreDefFontC predeffont;
        CStringArray* array;
        SXFSplitFeatureC split(IN_Instance->EntityData);
        array = split.Split();
        if(array == NULL)
            return FALSE;
        if (array->GetSize() != 1) // syntax err
            return FALSE;
        CString predeffontname=(* array)[0];
		/* 既定義線種名でない */
        if (!predeffont.SetName(predeffontname)){
			SXFErrorMsgC::RecordError(SXFNOTPRETYPE,
										"PREDEFINED_LINETYPE",
										1,
										predeffontname);
			return FALSE;
		}
		/* 既定義線種名が既にﾃｰﾌﾞﾙ内にある */
		if (!CheckSamePreDefFont(predeffontname)){
			SXFErrorMsgC::RecordError(SXFSAMENAME,
										"PREDEFINED_LINETYPE",
										1,
										predeffontname);
			return FALSE;
		}
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
        SXFTableMapManagerC* mappingmanager = new SXFTableMapManagerC(); 
		/* 既定義線種情報をｾｯﾄ */
        mappingmanager->SetFeatureData(4,&predeffont);
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
        predeffont.m_MappingManager=mappingmanager;
		/* ｲﾝｽﾀﾝｽIDｾｯﾄ */
        predeffont.m_ID = IN_Instance->InstanceID;
		/* 既定義線種ﾃｰﾌﾞﾙに追加 */
        m_PreDefFontTable[predeffont.m_Code-1] = predeffont;
    }
	/** ユーザ定義線種 **/
	else if(IN_Instance->EntityName == "user_defined_font_feature"){
        SXFUserDefFontC userdeffont;
        userdeffont.m_Code = (int) m_UserDefFontTable.GetSize()+17;//17---
		/** 最大線種数を超えた場合エラー処理 **/
        if( userdeffont.m_Code > MAXLINETYPE){
			SXFErrorMsgC::RecordError(SXFMAXUSERLINETYPE,
										"USERDEFINED_LINETYPE",
										0);
			return FALSE;
		}
        CStringArray* array;
        SXFSplitFeatureC split(IN_Instance->EntityData);
        array = split.Split();
        if(array == NULL)
            return FALSE;
        if (array->GetSize() !=3 ) // syntax err
            return FALSE;
        userdeffont.m_Name = (* array)[0];
		/* ﾕｰｻﾞ定義線種名がすでにﾃｰﾌﾞﾙ内にある */
		if (!CheckSameUserDefFont(userdeffont.m_Name)){
			SXFErrorMsgC::RecordError(SXFSAMENAME,
										"USERDEFINED_LINETYPE",
										1,
										userdeffont.m_Name);
			return FALSE;
		}
        userdeffont.m_Segment=atoi((* array)[1]);
		/////////////////////////
		//Segmentのチェック
		/////////////////////////
		//セグメント数が２の倍数でないとき
		if ((userdeffont.m_Segment % 2) != 0){
			SXFErrorMsgC::RecordError(SXFSEGMENTERROR,
										"USERDEFINED_LINETYPE",
										1,
										userdeffont.m_Segment);
			return FALSE;
		}
		//セグメント数が２から８でないとき
		if ((userdeffont.m_Segment < MINSEGMENT) 
				|| (userdeffont.m_Segment > MAXSEGMENT)){
			SXFErrorMsgC::RecordError(SXFSEGMENTERROR,
										"USERDEFINED_LINETYPE",
										1,
										userdeffont.m_Segment);
			return FALSE;
		}
        CStringArray* arraysecond;
        (* array)[2].TrimLeft('(');
        (* array)[2].TrimRight(')');
        ComSplitStringC splitsecond((* array)[2]);
        arraysecond = splitsecond.Split(',');
        if(userdeffont.m_Segment != arraysecond->GetSize())
            return FALSE;//// syntax err
		double pitch;
        for(int i=0; i < userdeffont.m_Segment; i++){
			pitch = atof((* arraysecond)[i]);
			/* ピッチが規定範囲外 */
			if (double_check(pitch) < 0 ){
				SXFErrorMsgC::RecordError(SXFPITCHERROR,
											"USERDEFINED_LINETYPE",
											1,
											pitch);
				return FALSE;
			}
            userdeffont.m_Pitch.Add(pitch);
        }
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
        SXFTableMapManagerC* mappingmanager = new SXFTableMapManagerC(); 
		/* ﾕｰｻﾞ定義線種情報をｾｯﾄ */
        mappingmanager->SetFeatureData(5,&userdeffont);
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
        userdeffont.m_MappingManager=mappingmanager;
		/* ｲﾝｽﾀﾝｽIDｾｯﾄ */
        userdeffont.m_ID = IN_Instance->InstanceID;
		/* ﾕｰｻﾞ定義線種ﾃｰﾌﾞﾙに追加 */
        m_UserDefFontTable.Add(userdeffont);
    }
	/** 線幅 **/
	else if(IN_Instance->EntityName == "width_feature"){
        SXFWidthC width;
        CStringArray* array;
        SXFSplitFeatureC split(IN_Instance->EntityData);
        array = split.Split();
        if(array == NULL)
            return FALSE;
        if (array->GetSize() != 1 ) // syntax err
            return FALSE;
        double inwidth=atof( (* array)[0] );
		//線幅が規定範囲外
		if (double_check(inwidth) < 0){
			SXFErrorMsgC::RecordError(SXFWIDTHERROR,
										"LINE_WIDTH",
										1,
										inwidth);
			return FALSE;
		}
		double Eps, Angle_Eps;
		m_ComInfo->Get_Tolerance(Eps,Angle_Eps);
		/* 線幅に対応する線幅ｺｰﾄﾞを求める */
        int resaultcode = width.InquireCode(inwidth,Eps);
		/* 対応する線幅ｺｰﾄﾞがあれば(既定義) */
        if(resaultcode != 0){
			/* 線幅が既にﾃｰﾌﾞﾙ内にあれば */
            if(m_PreDefWidthFlagTable[resaultcode-1] == 1){
				SXFErrorMsgC::RecordError(SXFSAMEWIDTH,
											"LINE_WIDTH",
											1,
											inwidth);
				return FALSE;
            }
			/* 定義済みﾌﾗｸﾞをON */
            m_PreDefWidthFlagTable[resaultcode-1]=1;
			/* 線幅ｺｰﾄﾞをｾｯﾄ */
            width.m_Code = resaultcode;
            SXFPreWidthTableC prewidthtable;
            double prewidth;
			/* ｺｰﾄﾞに対応する線幅を求める */
            prewidthtable.GetPreWidth(resaultcode, prewidth); 
            width.m_Width = prewidth;
        }
		/* 対応する線幅ｺｰﾄﾞがない(既定義でない) */
		else{
			/* 最大線幅数を超えたとき */
            if (userdefwidthcode > MAXLINEWIDTH){//widthcode 1---16
				SXFErrorMsgC::RecordError(SXFMAXLINEWIDTH,
											"LINE_WIDTH",
											0);
                return FALSE;
            }
			/* 線幅ｺｰﾄﾞｾｯﾄ */
            width.m_Code = userdefwidthcode;
            userdefwidthcode++;
			/* 線幅ｾｯﾄ */
            width.m_Width=inwidth;
        }
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
        SXFTableMapManagerC* mappingmanager = new SXFTableMapManagerC(); 
		/* 線幅情報をｾｯﾄ */
        mappingmanager->SetFeatureData(6,&width);
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
        width.m_MappingManager=mappingmanager;
		/* ｲﾝｽﾀﾝｽIDｾｯﾄ */
        width.m_ID = IN_Instance->InstanceID;
		/* 線幅ﾃｰﾌﾞﾙ追加 */
        m_WidthTable[width.m_Code-1] = width;
    }
	/** 文字フォント **/
	else if(IN_Instance->EntityName == "text_font_feature"){       
        SXFTextFontC textfont;
        CStringArray* array;
        SXFSplitFeatureC split(IN_Instance->EntityData);
        array = split.Split();
        if(array == NULL)
            return FALSE;
        if (array->GetSize() != 1 ) // syntax err
            return FALSE;
        textfont.m_Name = (* array)[0];
		//文字フォント名の長さが０はエラー
		if (textfont.m_Name.GetLength() <= 0){
			SXFErrorMsgC::RecordError(SXFFONTNAMEERROR,
										"TEXT_FONT",
										0);
			return FALSE;
		}
		/* 文字ﾌｫﾝﾄ名が既にﾃｰﾌﾞﾙにある */
		if (!CheckSameTextFont(textfont.m_Name)){
			SXFErrorMsgC::RecordError(SXFSAMENAME,
										"TEXT_FONT",
										1,
										textfont.m_Name);
			return FALSE;
		}
		/* 文字ﾌｫﾝﾄｺｰﾄﾞｾｯﾄ */
        textfont.m_Code = (int) m_TextFontTable.GetSize()+1;        
		/* 最大文字ﾌｫﾝﾄ数を超えた場合 */
        if (textfont.m_Code > MAXTEXTFONT){//textfont 1--1024
            //error
			SXFErrorMsgC::RecordError(SXFMAXTEXTFONT,
										"TEXT_FONT",
										0);
			return FALSE;
        }
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
        SXFTableMapManagerC* mappingmanager = new SXFTableMapManagerC(); 
		/* 文字ﾌｫﾝﾄ情報ｾｯﾄ */
        mappingmanager->SetFeatureData(7,&textfont);
		/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
        textfont.m_MappingManager=mappingmanager;
		/* ｲﾝｽﾀﾝｽIDｾｯﾄ */
        textfont.m_ID = IN_Instance->InstanceID;
		/* 文字ﾌｫﾝﾄﾃｰﾌﾞﾙに追加 */
        m_TextFontTable.Add(textfont);
    }
    return TRUE;

}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      Table 要素の構造体を登録する(WRITE)
//      
//  ２．インターフェース
//      int RegisterStructTable( int IN_Type, LPVOID IN_Struct)
//
//	    引数: 型			  引数名			    内容
//            int             IN_Type              構造体のType
//            LPVOID          IN_Struct            構造体のアドレス
//                                                  
//      
//	    復帰値	
//            正常時： Table 要素のCode
//			  異常時： 負(エラーコード)
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
int SXFTableManagerC::RegisterStructTable( int IN_Type, LPVOID IN_Struct)
{
	int return_code;
	SXFTableMapManagerC* mappingmanager; 
	SXFLayerC layer;
	SXFPreDefColorC predefcolor;
	SXFUserDefColorC userdefcolor;
	SXFPreDefFontC predeffont;
	SXFUserDefFontC userdeffont;
	SXFWidthC width;
	double inwidth;
	int resaultcode;
	SXFTextFontC textfont;
	int i = 0;
	INT_PTR layersize;
	INT_PTR usercolorsize;
	INT_PTR userfontsize;
	INT_PTR textfontsize;
	double eps1,eps2;
	int InstanceID;

	switch(IN_Type)
    {
		/** レイヤ **/
		case 1:
			/* ﾚｲﾔﾃｰﾌﾞﾙｻｲｽﾞ */
			layersize = m_LayerTable.GetSize();
			/* 最大ﾚｲﾔ数を超えたとき */
			if (layersize >= MAXLAYER){// max layer code = 256 
				SXFErrorMsgC::RecordError(SXFMAXLAYER,"LAYER",0);
				return SXFMAXLAYER;
			}
			/* ﾚｲﾔｺｰﾄﾞ */
			layer.m_Code = (int)layersize+1;
			/* ﾚｲﾔ名 */
			layer.m_Name = ((Layer_Struct*)IN_Struct)->name;
			/* 表示／非表示ﾌﾗｸﾞ */
			layer.m_LfFlag = ((Layer_Struct*)IN_Struct)->lflag;
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
			mappingmanager = new SXFTableMapManagerC(m_WriteFile,
														m_InstanceIDManager);
			/* 復帰値(ﾚｲﾔｺｰﾄﾞ) */
			return_code = layer.m_Code;
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
			layer.m_MappingManager = mappingmanager;
			/* ﾚｲﾔﾃｰﾌﾞﾙに追加 */
			m_LayerTable.Add(layer);
			break;
		/** 既定義色 **/
		case 2://predefcolor 16
			//既定義でない名前はエラー
			if (!predefcolor.SetName(((Predefined_Colour_Struct*)IN_Struct)->name)){
				SXFErrorMsgC::RecordError(SXFNOTPRECOLOUR,
											"PREDEFINED_COLOUR",
											1,
											((Predefined_Colour_Struct*)IN_Struct)->name);
				return SXFNOTPRECOLOUR;
			}
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
			mappingmanager = new SXFTableMapManagerC(m_WriteFile,
														m_InstanceIDManager);
			/* 既定義色構造体ｾｯﾄ */
			mappingmanager->SetStructPrecolor(IN_Type,
												IN_Struct,
												predefcolor);
			/* 既定義色ｲﾝｽﾀﾝｽ出力 */
			return_code = mappingmanager->WriteInstance(&InstanceID);
			/* 出力失敗 */
			if (return_code){
				delete mappingmanager;
				break;
			}
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
			predefcolor.m_MappingManager=mappingmanager;
			/* 復帰値(色ｺｰﾄﾞ) */
			return_code = predefcolor.m_Code;
			/* ｲﾝｽﾀﾝｽID */
			predefcolor.m_ID = InstanceID;
			/* 既定義色ﾃｰﾌﾞﾙに追加 */
			m_PreDefColorTable[predefcolor.m_Code-1] = predefcolor;
			break;
		/** ユーザ定義色 **/
		case 3://userdefcolor
			/* ﾕｰｻﾞ定義色ﾃｰﾌﾞﾙｻｲｽﾞ */
			usercolorsize = m_UserDefColorTable.GetSize();
			///ERROR
			//ユーザ定義は240色しか定義できない。
			if (usercolorsize >= MAXUSERCOLOUR){
				SXFErrorMsgC::RecordError(SXFMAXUSERCOLOUR,
											"USERDEFINED_COLOUR",
											0);
				return SXFMAXUSERCOLOUR;
			}
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ */
			mappingmanager = new SXFTableMapManagerC(m_WriteFile,
													m_InstanceIDManager);
			/* 色ｺｰﾄﾞ */
			userdefcolor.m_Code = (int)usercolorsize+17;//17------
			/* RGB値 */
			userdefcolor.R =((Userdefined_Colour_Struct*)IN_Struct)->red;
			userdefcolor.G =((Userdefined_Colour_Struct*)IN_Struct)->green;
			userdefcolor.B =((Userdefined_Colour_Struct*)IN_Struct)->blue;
			/* ﾕｰｻﾞ定義色構造体ｾｯﾄ */
			mappingmanager->SetStructData(IN_Type,IN_Struct);
			/* ﾕｰｻﾞ定義色ｲﾝｽﾀﾝｽ出力 */
			return_code = mappingmanager->WriteInstance(&InstanceID);
			/* 出力失敗 */
			if (return_code){
				delete mappingmanager;
				break;
			}
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
			userdefcolor.m_MappingManager=mappingmanager;
			/* 復帰値(色ｺｰﾄﾞ) */
			return_code = userdefcolor.m_Code;
			/* ｲﾝｽﾀﾝｽIDｾｯﾄ */
			userdefcolor.m_ID = InstanceID;
			/* ﾕｰｻﾞ定義色ﾃｰﾌﾞﾙに追加 */
			m_UserDefColorTable.Add(userdefcolor);
			break;
		/** 既定義線種 **/
		case 4://predeffont
			//既定義でない名前はエラー
			if (!predeffont.SetName(((Predefined_Linetype_Struct*)IN_Struct)->name)){
				SXFErrorMsgC::RecordError(SXFNOTPRETYPE,
										"PREDEFINED_LINETYPE",
										1,
										((Predefined_Linetype_Struct*)IN_Struct)->name);
				return SXFNOTPRETYPE;
			}
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
			mappingmanager = new SXFTableMapManagerC(m_WriteFile,
													m_InstanceIDManager);
			/* 既定義線種構造体ｾｯﾄ */
			mappingmanager->SetStructData(IN_Type,IN_Struct);
			/* 既定義線種ｲﾝｽﾀﾝｽ出力 */
			return_code = mappingmanager->WriteInstance(&InstanceID);
			/* 出力失敗 */
			if (return_code){
				delete mappingmanager;
				break;
			}
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
			predeffont.m_MappingManager=mappingmanager;
			/* 復帰値(線種ｺｰﾄﾞ) */
			return_code = predeffont.m_Code;
			/* ｲﾝｽﾀﾝｽID */
			predeffont.m_ID = InstanceID;
			/* 既定義線種ﾃｰﾌﾞﾙ追加 */
			m_PreDefFontTable[predeffont.m_Code-1] = predeffont;
			break;
		/** ユーザ定義線種 **/
		case 5://userdeffont
			/* ﾕｰｻﾞ定義線種ﾃｰﾌﾞﾙｻｲｽﾞ */
			userfontsize = m_UserDefFontTable.GetSize();
			///ERROR
			//ユーザ定義線種は32個しか定義できない。
			if (userfontsize >= MAXUSERLINETYPE){
				SXFErrorMsgC::RecordError(SXFMAXUSERLINETYPE,
											"USERDEFINED_LINETYPE",
											0);
				return SXFMAXUSERLINETYPE;
			}
			/* 線種ｺｰﾄﾞ */
			userdeffont.m_Code = (int) userfontsize+17;//17---
			/* 線種名 */
			userdeffont.m_Name = ((Userdefined_Linetype_Struct*)IN_Struct)->name;
			/* ｾｸﾞﾒﾝﾄ数 */
			userdeffont.m_Segment=((Userdefined_Linetype_Struct*)IN_Struct)->segment;
			/* ﾋﾟｯﾁ */
			for(i=0;i<userdeffont.m_Segment;i++)
				userdeffont.m_Pitch.Add( ((Userdefined_Linetype_Struct*)IN_Struct)->pitch[i] );
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
			mappingmanager = new SXFTableMapManagerC(m_WriteFile,m_InstanceIDManager);
			/* ﾕｰｻﾞ定義線種構造体ｾｯﾄ */
			mappingmanager->SetStructData(IN_Type,IN_Struct);
			/* ﾕｰｻﾞ定義線種ｲﾝｽﾀﾝｽ出力 */
			return_code = mappingmanager->WriteInstance(&InstanceID);
			/* 出力失敗 */
			if (return_code){
				delete mappingmanager;
				break;
			}
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
			userdeffont.m_MappingManager=mappingmanager;
			/* 返却値(線種ｺｰﾄﾞ) */
			return_code = userdeffont.m_Code;
			/* ｲﾝｽﾀﾝｽID */
			userdeffont.m_ID = InstanceID;
			/* ﾕｰｻﾞ定義線種ﾃｰﾌﾞﾙ追加 */
			m_UserDefFontTable.Add(userdeffont);
			break;
		/** 線幅 **/
		case 6://width
			/* 線幅 */
			inwidth = ((Line_Width_Struct*)IN_Struct)->width;
			/* 許容誤差取得 */
			m_ComInfo->Get_Tolerance(eps1,eps2);
			/* 線幅から線幅ｺｰﾄﾞを求める */
			resaultcode = width.InquireCode(inwidth,eps1);
			/* 線幅に対応する線幅ｺｰﾄﾞがある(既定義) */
			if(resaultcode != 0){
				/* 既にﾃｰﾌﾞﾙ内にある */
				if(m_PreDefWidthFlagTable[resaultcode-1] == 1){
					SXFErrorMsgC::RecordError(SXFSAMEWIDTH,
												"LINE_WIDTH",
												1,
												inwidth);
					return SXFSAMEWIDTH;
				}
				/* 定義済みﾌﾗｸﾞをON */
				m_PreDefWidthFlagTable[resaultcode-1]=1;
				/* 線幅ｺｰﾄﾞ */
				width.m_Code = resaultcode;
				SXFPreWidthTableC prewidthtable;
				double prewidth = 0.0;
				/* 線幅ｺｰﾄﾞから線幅を取得 */
				prewidthtable.GetPreWidth(resaultcode, prewidth); 
				/* 線幅 */
				width.m_Width = prewidth;
			}
			//線幅に対応するコードが０のときは既定義ではない。
			else{
				//線幅が１６以上登録されていれば
				//それ以上登録できないので、エラー
				if (userdefwidthcode > MAXLINEWIDTH){//widthcode 1---16
  					SXFErrorMsgC::RecordError(SXFMAXLINEWIDTH,
												"LINE_WIDTH",
												0);
					return SXFMAXLINEWIDTH;
				}
				/* 線幅ｺｰﾄﾞ */
				width.m_Code = userdefwidthcode;
				userdefwidthcode++;
				/* 線幅 */
				width.m_Width=inwidth;
			}
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
			mappingmanager = new SXFTableMapManagerC(m_WriteFile,
														m_InstanceIDManager);
			/* 線幅構造体ｾｯﾄ */
			mappingmanager->SetStructData(IN_Type,IN_Struct);
			/* 線幅ｲﾝｽﾀﾝｽ出力 */
			return_code = mappingmanager->WriteInstance(&InstanceID);
			/* 出力失敗 */
			if (return_code){
				delete mappingmanager;
				break;
			}
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
			width.m_MappingManager=mappingmanager;
			/* 復帰値(線幅ｺｰﾄﾞ) */
			return_code = width.m_Code;
			/* ｲﾝｽﾀﾝｽID */
			width.m_ID = InstanceID;
			/* 線幅ﾃｰﾌﾞﾙに追加 */
			m_WidthTable[width.m_Code-1] = width;
			break;
		/** 文字フォント **/
		case 7://textfont
			//ERROR
			//文字フォントは１０２４個しか登録できない
			textfontsize = m_TextFontTable.GetSize();
			if (textfontsize >= MAXTEXTFONT){//textfont 1--1024
				SXFErrorMsgC::RecordError(SXFMAXTEXTFONT,"TEXT_FONT",0);
				return SXFMAXTEXTFONT;
			}
			/* 文字ﾌｫﾝﾄ名 */
			textfont.m_Name = ((Text_Font_Struct*)IN_Struct)->name;
			/* 文字ﾌｫﾝﾄｺｰﾄﾞ */
			textfont.m_Code = (int) textfontsize+1;
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
			mappingmanager = new SXFTableMapManagerC(m_WriteFile,
													m_InstanceIDManager);
			/* 文字ﾌｫﾝﾄ構造体ｾｯﾄ */
			mappingmanager->SetStructData(IN_Type,IN_Struct);
			/* 文字ﾌｫﾝﾄｲﾝｽﾀﾝｽ出力 */
			return_code = mappingmanager->WriteInstance(&InstanceID);
			/* 出力失敗 */
			if (return_code){
				delete mappingmanager;
				break;
			}
			/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
			textfont.m_MappingManager=mappingmanager;
			/* 文字ﾌｫﾝﾄｺｰﾄﾞ */
			return_code = textfont.m_Code;
			/* ｲﾝｽﾀﾝｽID */
			textfont.m_ID = InstanceID;
			/* 文字ﾌｫﾝﾄﾃｰﾌﾞﾙに追加 */
			m_TextFontTable.Add(textfont);
			break;
    }
    return return_code;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      指定したTable要素のCodeが定義されているかチェック
//      
//  ２．インターフェース
//      int GetID(int IN_Type, int IN_Code)
//
//	    引数: 型			  引数名			    内容
//            int             IN_Type              Table要素のType
//            int             IN_Code              Table要素のCode
//                                                  
//      
//	    復帰値	
//            正常時： 正	:定義されている
//			  異常時： -1	:定義されていない
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
int SXFTableManagerC::GetID(int IN_Type, int IN_Code)
{
    int getid,RW_Flag;

    if(IN_Code<1)  //code must >=1
		return -1;
	/* 読書きﾌﾗｸﾞ取得 */
	m_ComInfo->Get_RWFlag(RW_Flag);

    getid=-1;
    switch (IN_Type){
		case 1://layer
			if(IN_Code > m_LayerTable.GetSize())
				return -1;//layer code not register
			getid = m_LayerTable[IN_Code-1].m_LfFlag;//テーブルに登録されていなければ初期値-１
			break;
		case 2://predefcolor
			if(IN_Code>16)
				return -1;//predefcolor must <= 16
			getid = m_PreDefColorTable[IN_Code-1].m_ID;
			break;
		case 3://userdefcolor
			if(IN_Code<17 || IN_Code>=m_UserDefColorTable.GetSize()+17)
				return -1;//userdefcolor code must 17---256
						  //or user def color not register
			getid=m_UserDefColorTable[IN_Code-17].m_ID;
			break;
		case 4://predeffont
			if(IN_Code>16)
				return -1;//predeffont code must <=16
			getid = m_PreDefFontTable[IN_Code-1].m_ID;
			break;
		case 5://userdeffont
			if(IN_Code<17 || IN_Code>=m_UserDefFontTable.GetSize()+17)
				return -1;//user def font code 17---32
						  //or user def font not register
			getid = m_UserDefFontTable[IN_Code-17].m_ID;
			break;
		case 6://width
			if(IN_Code>16)
				return -1;//width code 1--16
			getid=m_WidthTable[IN_Code-1].m_ID;
			break;
		case 7://textfont
			if(IN_Code>m_TextFontTable.GetSize())
				return -1;//text font must 1--1024
						  //text font not register
			getid=m_TextFontTable[IN_Code-1].m_ID;
			break;
		default:
			break;
	} 
    return getid;//-1:error, ID:OK
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      指定したLayer Nameが定義されているかチェック
//      
//  ２．インターフェース
//      BOOL CheckSameLayer( LPCTSTR IN_Name)
//
//	    引数: 型			  引数名			    内容
//            LPCTSTR        IN_Name               Layer Name                        
//      
//	    復帰値	
//            正常時： TRUE
//			  異常時： FALSE
//      
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
BOOL SXFTableManagerC::CheckSameLayer( LPCTSTR IN_Name)
{
    for(int i=0;i<m_LayerTable.GetSize();i++){
        if(IN_Name == m_LayerTable[i].m_Name)
            return FALSE;
    }
    
    return TRUE;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      指定したPreDefColor Nameが定義されているかチェック
//      
//  ２．インターフェース
//      BOOL CheckSamePreDefColor( LPCTSTR IN_Name)
//
//	    引数: 型			  引数名			    内容
//            LPCTSTR        IN_Name               PreDefColor Name                        
//      
//	    復帰値	
//            正常時： TRUE
//			  異常時： FALSE
//      
//  ２．インターフェース
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
BOOL SXFTableManagerC::CheckSamePreDefColor( LPCTSTR IN_Name)
{
    for(int i=0;i<16;i++){
        if(IN_Name == m_PreDefColorTable[i].m_Name)
            return FALSE;
    }

    return TRUE;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      指定したPreDefFont Nameが定義されているかチェック
//      
//  ２．インターフェース
//      BOOL CheckSamePreDefFont( LPCTSTR IN_Name)
//
//	    引数: 型			  引数名			    内容
//            LPCTSTR        IN_Name               PreDefFont Name                        
//      
//	    復帰値	
//            正常時： TRUE
//			  異常時： FALSE
//      
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
BOOL SXFTableManagerC::CheckSamePreDefFont( LPCTSTR IN_Name)
{
    for(int i=0;i<16;i++){
        if(IN_Name == m_PreDefFontTable[i].m_Name)
            return FALSE;
    }
    return TRUE;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      指定したUserDefFont Nameが定義されているかチェック
//      
//  ２．インターフェース
//      BOOL CheckSameUserDefFont( LPCTSTR IN_Name)
//
//	    引数: 型			  引数名			    内容
//            LPCTSTR        IN_Name               UserDefFont Name                        
//      
//	    復帰値	
//            正常時： TRUE
//			  異常時： FALSE
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
BOOL SXFTableManagerC::CheckSameUserDefFont( LPCTSTR IN_Name)
{
    for(int i=0;i<m_UserDefFontTable.GetSize();i++){
        if(IN_Name == m_UserDefFontTable[i].m_Name)
            return FALSE;
    }

    return TRUE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      指定したWidth値が定義されているかチェック
//      
//  ２．インターフェース
//      BOOL CheckSameWidth( double IN_Width)
//
//	    引数: 型			  引数名			    内容
//            double         IN_Width              Width値                        
//      
//	    復帰値	
//            正常時： TRUE
//			  異常時： FALSE
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
BOOL SXFTableManagerC::CheckSameWidth( double IN_Width)
{
	double eps,angle_eps;
	m_ComInfo->Get_Tolerance(eps,angle_eps);
    SXFPreWidthTableC prewidthtable;
    int code = prewidthtable.GetCode(IN_Width,eps);
    if(m_PreDefWidthFlagTable[code-1] == 1){
        return FALSE;
    }
    return TRUE;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      指定したTextFontが定義されているかチェック
//      
//  ２．インターフェース
//      BOOL CheckSameTextFont( LPCTSTR IN_Name)
//
//	    引数: 型			  引数名			    内容
//            LPCTSTR        IN_Name                TextFont Name                        
//      
//	    復帰値	
//            正常時： TRUE
//			  異常時： FALSE
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------

BOOL SXFTableManagerC::CheckSameTextFont( LPCTSTR IN_Name)
{
    for(int i=0;i<m_TextFontTable.GetSize();i++){
        if(IN_Name == m_TextFontTable[i].m_Name)
            return FALSE;
    }

    return TRUE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      登録したLayer要素をMappingする
//      
//  ２．インターフェース
//      void  LayerMapping()
//
//	    引数: 型			  引数名			    内容
//      
//	    復帰値	
//            なし
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
void  SXFTableManagerC::LayerMapping()
{
	int InstanceID;

	for(int i=0;i<m_LayerTable.GetSize();i++){
		/* ﾚｲﾔ情報ｾｯﾄ */
		if (m_LayerTable[i].m_MappingManager->SetStructLayer(&m_LayerTable[i],&InstanceID))
			/* ｲﾝｽﾀﾝｽIDｾｯﾄ */
			m_LayerTable[i].m_ID = InstanceID;
	}
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableManagerC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      管理Tableから登録したTable要素を一つ出力する
//
//  ２．インターフェース
//      int GetTableElement(int IN_Type,
//                          LPVOID& OUT_Struct,
//                          int& OUT_EndFlg)
//
//	    引数: 型			  引数名			    内容
//            int            IN_Type               Table要素のType
//            LPVOID&        OUT_Struct            Table要素の構造体
//                                                 のアドレス
//            int&           OUT_EndFlg            最後の要素のFlag
//                                                  1:最後の要素
//                                                  0:続き
//
//	    復帰値	
//            正常時： Table要素のCode
//			  異常時： 負(エラーコード)
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
int SXFTableManagerC::GetTableElement(int IN_Type, LPVOID OUT_Struct, int* OUT_EndFlg)
{
    *OUT_EndFlg = 1;
	SXFTableMapManagerC* mappingmanager;
	int i = 0;
	CUIntArray predefcolor;
	CUIntArray predeffont;
	CUIntArray width;
	int return_code = -1;
	INT_PTR table_size;

	/* ﾃｰﾌﾞﾙ型が異常 */
	if (IN_Type < 1 || 7 < IN_Type){
		SXFErrorMsgC::RecordError(SXFTABLETYPEERR,"",1,IN_Type);
		return SXFTABLETYPEERR;
	}
	/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽが0 */
    if(m_CurrentNumber == 0 ){
        m_CurrentType = IN_Type;
    }
	else{
        if(m_CurrentType != IN_Type){
		    m_CurrentNumber = 0;
	        m_CurrentType = IN_Type;
		}
    }
    switch (m_CurrentType){
		/* ﾚｲﾔ */
		case 1://layer
			/* ﾚｲﾔﾃｰﾌﾞﾙのｻｲｽﾞ */
			table_size = m_LayerTable.GetSize();
			/* ｻｲｽﾞが0のとき */
			if (table_size == 0){
				/* 読み出すレイヤがありません */
//	MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOLAYER,"LAYER",0);
				SXFErrorMsgC::RecordError2(SXFNOLAYER,"LAYER",0);
				return SXFNOLAYER;
//	MOD(E)	メッセージ修正	K.Naono	04.03.05
			}
			/* ﾚｲﾔﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽがﾃｰﾌﾞﾙの最後 */
			if(m_CurrentNumber == table_size - 1)
				*OUT_EndFlg = 1;	/* EndFlgをON */
			else if(m_CurrentNumber > table_size - 1) {
				*OUT_EndFlg = 1;	/* EndFlgをON */
				/* 読み出すレイヤがありません */
				SXFErrorMsgC::RecordError(SXFNOLAYER,"LAYER",0);
				return SXFNOLAYER;
			}
			else
				*OUT_EndFlg = 0;	/* EndFlgをOFF */
			/* ﾚｲﾔ構造体 */
			*(Layer_Struct*)OUT_Struct = *(Layer_Struct*)(m_LayerTable[m_CurrentNumber].m_MappingManager->GetStructData(  ));
			/* 復帰値(ﾚｲﾔｺｰﾄﾞ) */
			return_code = m_LayerTable[m_CurrentNumber].m_Code;
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽを＋１ */
			m_CurrentNumber++;
			break;
		/* 既定義色 */
		case 2://predefcolor
			for(i=0; i<MAXPRECOLOUR; i++){
				/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
				mappingmanager = m_PreDefColorTable[i].m_MappingManager;
				if(mappingmanager != NULL)
					/* 既定義色ﾃｰﾌﾞﾙに追加 */
					predefcolor.Add(i);
			}
			/* 既定義色ﾃｰﾌﾞﾙｻｲｽﾞ */
			table_size = predefcolor.GetSize();
			/* ﾃｰﾌﾞﾙｻｲｽﾞが0 */
			if (table_size == 0){
				/* 読み出す既定義色がありません */
//	MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOPRECOLOUR,"PREDEFINED_COLOUR",0);
				SXFErrorMsgC::RecordError2(SXFNOPRECOLOUR,"PREDEFINED_COLOUR",0);
				return SXFNOPRECOLOUR;
//	MOD(E)	メッセージ修正	K.Naono	04.03.05
			}
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽが最後 */
			if(m_CurrentNumber == table_size-1)
				*OUT_EndFlg = 1;	/* EndFlgをON */
			else if(m_CurrentNumber > table_size-1) {
				*OUT_EndFlg = 1;	/* EndFlgをON */
				/* 読み出す既定義色がありません */
				SXFErrorMsgC::RecordError(SXFNOPRECOLOUR,"PREDEFINED_COLOUR",0);
				return SXFNOPRECOLOUR;
			}
			else
				*OUT_EndFlg = 0;	/* EndFlgをOFF */
			/* 既定義色構造体 */
			*(Predefined_Colour_Struct*)OUT_Struct = *(Predefined_Colour_Struct*)(m_PreDefColorTable[predefcolor[m_CurrentNumber]].m_MappingManager->GetStructData(  ));
			/* 復帰値(色ｺｰﾄﾞ) */
			return_code = m_PreDefColorTable[predefcolor[m_CurrentNumber]].m_Code;
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽを＋１ */
			m_CurrentNumber++;
			break;
		/* ﾕｰｻﾞ定義色 */
		case 3://userdefcolor
			/* ﾕｰｻﾞ定義色ﾃｰﾌﾞﾙｻｲｽﾞ */
			table_size = m_UserDefColorTable.GetSize();
			/* ﾃｰﾌﾞﾙｻｲｽﾞが0 */
			if (table_size == 0){
				/* 読み出すﾕｰｻﾞ定義色がありません */
//	MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOUSERCOLOUR,"USERDEFINED_COLOUR",0);
				SXFErrorMsgC::RecordError2(SXFNOUSERCOLOUR,"USERDEFINED_COLOUR",0);
				return SXFNOUSERCOLOUR;
//	MOD(E)	メッセージ修正	K.Naono	04.03.05
			}
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽが最後 */
			if(m_CurrentNumber == table_size-1)
				*OUT_EndFlg = 1;	/* EndFlgをON */
			else if(m_CurrentNumber > table_size-1) {
				*OUT_EndFlg = 1;	/* EndFlgをON */
				/* 読み出すﾕｰｻﾞ定義色がありません */
				SXFErrorMsgC::RecordError(SXFNOUSERCOLOUR,"USERDEFINED_COLOUR",0);
				return SXFNOUSERCOLOUR;
			}
			else
				*OUT_EndFlg = 0;	/* EndFlgをOFF */
			/* ﾕｰｻﾞ定義色構造体 */
			*(Userdefined_Colour_Struct*)OUT_Struct = *(Userdefined_Colour_Struct*)(m_UserDefColorTable[m_CurrentNumber].m_MappingManager->GetStructData(  ));
			/* 復帰値(色ｺｰﾄﾞ) */
			return_code = m_UserDefColorTable[m_CurrentNumber].m_Code;
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽを＋１ */
			m_CurrentNumber++;
			break;
		/* 既定義線種 */
		case 4://predeffont
			for(i=0; i<MAXPRELINETYPE; i++){
				/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄ生成 */
				mappingmanager = m_PreDefFontTable[i].m_MappingManager;
				if(mappingmanager != NULL)
					/* 既定義線種ﾃｰﾌﾞﾙに追加 */
					predeffont.Add(i);
			}
			/* 既定義線種ﾃｰﾌﾞﾙｻｲｽﾞ */
			table_size = predeffont.GetSize();
			/* ﾃｰﾌﾞﾙｻｲｽﾞが0 */
			if (table_size == 0){
				/* 読み出す既定義線種がありません */
//	MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOPRELINETYPE,"PREDEFINED_LINETYPE",0);
				SXFErrorMsgC::RecordError2(SXFNOPRELINETYPE,"PREDEFINED_LINETYPE",0);
				return SXFNOPRELINETYPE;
//	MOD(E)	メッセージ修正	K.Naono	04.03.05
			}
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽがﾃｰﾌﾞﾙの最後 */
			if(m_CurrentNumber == table_size-1)
				*OUT_EndFlg = 1;	/* EndFlgをON */
			else if(m_CurrentNumber > table_size-1) {
				*OUT_EndFlg = 1;	/* EndFlgをON */
				/* 読み出す既定義線種がありません */
				SXFErrorMsgC::RecordError(SXFNOPRELINETYPE,"PREDEFINED_LINETYPE",0);
				return SXFNOPRELINETYPE;
			}
			else
				*OUT_EndFlg = 0;	/* EndFlgをOFF */
			/* 既定義線種構造体 */
			*(Predefined_Linetype_Struct*)OUT_Struct = *(Predefined_Linetype_Struct*)(m_PreDefFontTable[predeffont[m_CurrentNumber]].m_MappingManager->GetStructData(  ));
			/* 復帰値(線種ｺｰﾄﾞ) */
			return_code = m_PreDefFontTable[predeffont[m_CurrentNumber]].m_Code;
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽを＋１ */
			m_CurrentNumber++;
			break;
		/* ﾕｰｻﾞ定義線種 */
		case 5://userdeffont
			/* ﾕｰｻﾞ定義線種ﾃｰﾌﾞﾙｻｲｽﾞ */
			table_size = m_UserDefFontTable.GetSize();
			/* ﾃｰﾌﾞﾙｻｲｽﾞが0 */
			if (table_size == 0){
				/* 読み出すﾕｰｻﾞ定義線種がありません */
//	MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOUSERLINETYPE,"USERDEFINED_LINETYPE",0);
				SXFErrorMsgC::RecordError2(SXFNOUSERLINETYPE,"USERDEFINED_LINETYPE",0);
				return SXFNOUSERLINETYPE;
//	MOD(E)	メッセージ修正	K.Naono	04.03.05
			}
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽがﾃｰﾌﾞﾙの最後 */
			if(m_CurrentNumber == table_size-1)
				*OUT_EndFlg = 1;		/* EndFlg をON */
			else if(m_CurrentNumber > table_size-1) {
				*OUT_EndFlg = 1;		/* EndFlg をON */
				/* 読み出すﾕｰｻﾞ定義線種がありません */
				SXFErrorMsgC::RecordError(SXFNOUSERLINETYPE,"USERDEFINED_LINETYPE",0);
				return SXFNOUSERLINETYPE;
			}
			else
				*OUT_EndFlg = 0;		/* EndFlg をOFF */
			/* ﾕｰｻﾞ定義線種構造体 */
			*(Userdefined_Linetype_Struct*)OUT_Struct = *(Userdefined_Linetype_Struct*)(m_UserDefFontTable[m_CurrentNumber].m_MappingManager->GetStructData(  ));
			/* 復帰値(線種ｺｰﾄﾞ) */
			return_code = m_UserDefFontTable[m_CurrentNumber].m_Code;
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽを＋１ */
			m_CurrentNumber++;
			break;
		/* 線幅 */
		case 6://width
			for(i=0; i<MAXLINEWIDTH; i++){
				/* 定義ﾃｰﾌﾞﾙ要素Mapping管理ｵﾌﾞｼﾞｪｸﾄｾｯﾄ */
				mappingmanager = m_WidthTable[i].m_MappingManager;
				if(mappingmanager != NULL)
					/* 線幅ﾃｰﾌﾞﾙに追加 */
					width.Add(i);
			}
			/* 線幅ﾃｰﾌﾞﾙｻｲｽﾞ */
			table_size = width.GetSize();
			/* ﾃｰﾌﾞﾙｻｲｽﾞが0 */
			if (table_size == 0){
				/* 読み出す線幅がありません */
//	MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOLINEWIDTH,"LINE_WIDTH",0);
				SXFErrorMsgC::RecordError2(SXFNOLINEWIDTH,"LINE_WIDTH",0);
				return SXFNOLINEWIDTH;
//	MOD(E)	メッセージ修正	K.Naono	04.03.05
			}
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽがﾃｰﾌﾞﾙの最後 */        
			if(m_CurrentNumber == table_size-1)
				*OUT_EndFlg = 1;	/* EndFlgをON */
			else if(m_CurrentNumber > table_size-1) {
				*OUT_EndFlg = 1;	/* EndFlgをON */
				/* 読み出す線幅がありません */
				SXFErrorMsgC::RecordError(SXFNOLINEWIDTH,"LINE_WIDTH",0);
				return SXFNOLINEWIDTH;
			}
			else
				*OUT_EndFlg = 0;	/* EndFlgをOFF */
			/* 線幅構造体 */
			*(Line_Width_Struct*)OUT_Struct = *(Line_Width_Struct*)(m_WidthTable[width[m_CurrentNumber]].m_MappingManager->GetStructData(  ));
			/* 復帰値(線幅ｺｰﾄﾞ) */
			return_code = m_WidthTable[width[m_CurrentNumber]].m_Code;
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽを＋１ */
			m_CurrentNumber++;
			break;
		/* 文字ﾌｫﾝﾄ */
		case 7://textfont
			/* 文字ﾌｫﾝﾄﾃｰﾌﾞﾙｻｲｽﾞ */
			table_size = m_TextFontTable.GetSize();
			/* ﾃｰﾌﾞﾙｻｲｽﾞが0 */
			if (table_size == 0){
				/* 読み出す文字ﾌｫﾝﾄがありません */
//	MOD(S)	メッセージ修正	K.Naono	04.03.05
//				SXFErrorMsgC::RecordError(SXFNOTEXTFONT,"TEXT_FONT",0);
				SXFErrorMsgC::RecordError2(SXFNOTEXTFONT,"TEXT_FONT",0);
				return SXFNOTEXTFONT;
//	MOD(E)	メッセージ修正	K.Naono	04.03.05
			}
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽがﾃｰﾌﾞﾙの最後 */
			if(m_CurrentNumber == table_size-1)
				*OUT_EndFlg = 1;		/* EndFlg をON */
			else if(m_CurrentNumber > table_size-1) {
				*OUT_EndFlg = 1;		/* EndFlg をON */
				/* 読み出す文字ﾌｫﾝﾄがありません */
				SXFErrorMsgC::RecordError(SXFNOTEXTFONT,"TEXT_FONT",0);
				return SXFNOTEXTFONT;
			}
			else
				*OUT_EndFlg = 0;		/* EndFlg をOFF */
			/* 文字ﾌｫﾝﾄ構造体 */
			*(Text_Font_Struct*)OUT_Struct = *(Text_Font_Struct*)(m_TextFontTable[m_CurrentNumber].m_MappingManager->GetStructData(  ));
			/* 復帰値(文字ﾌｫﾝﾄｺｰﾄﾞ) */
			return_code = m_TextFontTable[m_CurrentNumber].m_Code;
			/* ﾃｰﾌﾞﾙｲﾝﾃﾞｯｸｽを＋１ */
			m_CurrentNumber++;
			break;
    }        
	return return_code;
}
