//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.04.17
//
//	１．クラス概要
//      一般要素フィーチャーデータを保持する     
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.17     周
//		更新		2002.02.20	K.Naono		有効桁数15桁対応
//
//------------------------------------------------------------

#include "stdafx.h"
#include "SXFFeatureC.h"
#include "SXFStruct.h"
#include "SXFInstanceIDC.h"
#include "SXFSplitFeatureC.h"
#include "ComSplitStringC.h"
#include "SXFErrorMsgC.h"
#include "SXFutyFunctionC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.04.17
//
//  １．機能概要
//      クラス構築
//
//  ２．インターフェース
//		void	SXFFeatureC::SXFFeatureC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.17     周
//
//------------------------------------------------------------
SXFFeatureC::SXFFeatureC()
{
    m_FeatureData.EntityData.Empty();
    m_FeatureData.EntityName.Empty();
    m_FeatureData.InstanceMode = -1;
    m_FeatureData.InstanceID = -1;
    m_StructData=NULL;
	m_ReturnFlag = 0;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.04.17
//
//  １．機能概要
//      コンストラクタ
//
//  ２．インターフェース
//		void	SXFFeatureC::SXFFeatureC(SXFInstanceIDC* IN_InstanceIDManager)
//		
//	引数	：	型					引数名					内容
//				SXFInstanceIDC*		IN_InstanceIDManager	インスタンスIDオブジェクト
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.17     周
//
//------------------------------------------------------------
SXFFeatureC::SXFFeatureC(SXFInstanceIDC* IN_InstanceIDManager)
{
	m_InstanceIDManager = IN_InstanceIDManager;
    m_FeatureData.EntityData.Empty();
    m_FeatureData.EntityName.Empty();
    m_FeatureData.InstanceMode = -1;
    m_FeatureData.InstanceID = -1;
    m_StructData=NULL;
	m_ReturnFlag = 0;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.04.17
//
//  １．機能概要
//      デストラクタ
//  ２．インターフェース
//		void	SXFFeatureC::~SXFFeatureC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.17     周
//
//------------------------------------------------------------
SXFFeatureC::~SXFFeatureC()
{
	if (!m_ReturnFlag && m_StructData){
		if (strcmp(m_TypeName,"POLYLINE") == 0)
			delete (Polyline_Struct*)m_StructData;
		else if (strcmp(m_TypeName,"SPLINE") == 0)
			delete (Spline_Struct*)m_StructData;
		else if (strcmp(m_TypeName,"CLOTHOID") == 0)
			delete (Clothoid_Struct*)m_StructData;
		else if (strcmp(m_TypeName,"LABEL") == 0)
			delete (Label_Struct*)m_StructData;
		else if (strcmp(m_TypeName,"BALLOON") == 0)
			delete (Balloon_Struct*)m_StructData;
		else if (strcmp(m_TypeName,"FILL_AREA_STYLE_HATCHING") == 0)
			delete (Fill_area_style_hatching_Struct*)m_StructData;
		else if (strcmp(m_TypeName,"EXTERNALLY_DEFINED_HATCH") == 0)
			delete (Externally_Defined_Hatch_Struct*)m_StructData;
		else if (strcmp(m_TypeName,"FILL_AREA_STYLE_COLOUR") == 0)
			delete (Fill_area_style_colour_Struct*)m_StructData;
		else if (strcmp(m_TypeName,"FILL_AREA_STYLE_TILES") == 0)
			delete (Fill_area_style_tiles_Struct*)m_StructData;
		else 
			delete m_StructData;
	}
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.04.17
//
//  １．機能概要
//      Feature Comment data から構造体に変換する(READ)
//
//  ２．インターフェース
//      BOOL SetStructData(INSTANCEDATA* IN_Feature)
//
//	    引数: 型			  引数名			    内容
//            INSTANCEDATA*   IN_Feature	       Instance Data
//                                                 構造体のアドレス
//	    復帰値	
//            正常時：TRUE
//			  異常時：FALSE
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.17     周
//
//------------------------------------------------------------
BOOL SXFFeatureC::SetStructData(INSTANCEDATA* IN_Feature)
{
    m_FeatureData = *IN_Feature;
    CStringArray* arrayfirst;
    CStringArray* arraysecond;
    CStringArray* arraythird;
	CString InsID;
	size_t ilen;			/* 文字列長 */

	//インスタンスIDを設定
	InsID.Format("#%d",IN_Feature->InstanceID);
	/** 用紙 **/
    if(IN_Feature->EntityName == "drawing_sheet_feature" ){//1
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 5)//syntax error
            return FALSE;
        //make up struct data
        Sheet_Struct* sheet = new Sheet_Struct;
		/* 用紙名の長さを調べる */
		ilen = strlen((*arrayfirst)[0]);
		//if (ilen <= 255){		bugfix	T.Matsushima	2015.04.27
		if (ilen <= 256){
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(sheet->name,(*arrayfirst)[0]);
			strcpy_s(sheet->name,_countof(sheet->name),(*arrayfirst)[0]);
		}else{
			/* メッセージを出力 */
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										"SHEET",
										2,
										InsID,
										"SHEET");
			/* 先頭から256バイト切り取る */
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(sheet->name,(*arrayfirst)[0],256);
			strncpy_s(sheet->name,_countof(sheet->name),(*arrayfirst)[0],256);
			sheet->name[256] = 0x00;
		}
        sheet->type = atoi( (*arrayfirst)[1] );
        sheet->orient = atoi( (*arrayfirst)[2] );

		//用紙サイズの設定
		int orgX = 0;
		int orgY = 0;
		//横
		if (sheet->orient == 1 || sheet->type == 9){
		orgX = atoi( (*arrayfirst)[3] );
		orgY = atoi( (*arrayfirst)[4] );
		}
		//縦
		else{
		orgX = atoi( (*arrayfirst)[4] );
		orgY = atoi( (*arrayfirst)[3] );
		}
	
		if((sheet->type>=0) && (sheet->type<=4)){
			sheet->x = GetSheetX(sheet->type);
			sheet->y = GetSheetY(sheet->type);
			//用紙サイズ種別０～４で、０以外の異なった用紙サイズが設定されていた場合、
			//メッセージのみ出力。
			if ( orgX != sheet->x && orgX != 0 ){
				SXFErrorMsgC::RecordError(SXFSHEETLENGTHWARNING,
											"SHEET",
											1,
											orgX);
			}
			if ( orgY != sheet->y && orgY != 0 ){
				SXFErrorMsgC::RecordError(SXFSHEETLENGTHWARNING,
											"SHEET",
											1,
											orgY);
			}
		}
		else {	
			sheet->x = orgX;
			sheet->y = orgY;
		}

        //save struct data ptr
        m_StructData = sheet;
    }
	/** 点マーカ **/
	else if(IN_Feature->EntityName == "point_marker_feature" ){//2
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "POINT_MARKER");
		strcpy_s(m_TypeName, _countof(m_TypeName), "POINT_MARKER");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 7)//syntax error
            return FALSE;
        //make up struct data
        Point_Marker_Struct* pointmarker = new Point_Marker_Struct;
        pointmarker->layer = atoi( (*arrayfirst)[0] );
        pointmarker->color = atoi( (*arrayfirst)[1] );
        pointmarker->start_x = atof( (*arrayfirst)[2] );
        pointmarker->start_y = atof( (*arrayfirst)[3] );
        pointmarker->marker_code = atoi( (*arrayfirst)[4] );
        pointmarker->rotate_angle = atof( (*arrayfirst)[5] );
        pointmarker->scale = atof( (*arrayfirst)[6] );
        //save struct data ptr
        m_StructData = pointmarker;
    }
	/** 線分 **/
	else if(IN_Feature->EntityName == "line_feature" ){//3
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "LINE");
		strcpy_s(m_TypeName, _countof(m_TypeName), "LINE");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 8)//syntax error
            return FALSE;
        //make up struct data
        Line_Struct* line = new Line_Struct;
        line->layer = atoi( (*arrayfirst)[0] );
        line->color = atoi( (*arrayfirst)[1] );
        line->type = atoi( (*arrayfirst)[2] );
        line->line_width = atoi( (*arrayfirst)[3] );
        line->start_x = atof( (*arrayfirst)[4] );
        line->start_y = atof( (*arrayfirst)[5] );
        line->end_x = atof( (*arrayfirst)[6] );
        line->end_y = atof( (*arrayfirst)[7] );
        //save struct data ptr
        m_StructData = line;
    }
	/** 折線 **/
	else if(IN_Feature->EntityName == "polyline_feature" ){//4
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "POLYLINE");
		strcpy_s(m_TypeName, _countof(m_TypeName), "POLYLINE");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 7)//syntax error
            return FALSE;
        //make up struct data
        Polyline_Struct* polyline = new Polyline_Struct;
        polyline->layer = atoi( (*arrayfirst)[0] );
        polyline->color = atoi( (*arrayfirst)[1] );
        polyline->type = atoi( (*arrayfirst)[2] );
        polyline->line_width = atoi( (*arrayfirst)[3] );
        polyline->number = atoi( (*arrayfirst)[4] );
        //add x
        (*arrayfirst)[5].TrimLeft('(');
        (*arrayfirst)[5].TrimRight(')');
        ComSplitStringC splitsecond((*arrayfirst)[5]);
        arraysecond = splitsecond.Split(',');
        INT_PTR arraysecondsize = arraysecond->GetSize();
        if (arraysecondsize != polyline->number)//syntax error
            return FALSE;
        for(int i=0;i<arraysecondsize;i++){
            polyline->x.Add(atof((*arraysecond)[i]));
        }
        //add y
        (*arrayfirst)[6].TrimLeft('(');
        (*arrayfirst)[6].TrimRight(')');
        ComSplitStringC splitthird((*arrayfirst)[6]);
        arraythird = splitthird.Split(',');
        INT_PTR arraythirdsize = arraythird->GetSize();
        if (arraythirdsize != polyline->number)//syntax error
		{
			SXFErrorMsgC::RecordError(SXFNOTVERTEXNUMBER,
										m_TypeName,
										3,
										InsID,
										polyline->number,
										m_TypeName);
			return FALSE;
		}
        for(i=0;i<arraythirdsize;i++){
            polyline->y.Add(atof((*arraythird)[i]));
        }
        //save struct data ptr
        m_StructData = polyline;
    }
	/** 円 **/
	else if(IN_Feature->EntityName == "circle_feature" ){//5
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "CIRCLE");
		strcpy_s(m_TypeName, _countof(m_TypeName), "CIRCLE");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 7)//syntax error
            return FALSE;
        //make up struct data
        Circle_Struct* circle = new Circle_Struct;
        circle->layer = atoi( (*arrayfirst)[0] );
        circle->color = atoi( (*arrayfirst)[1] );
        circle->type = atoi( (*arrayfirst)[2] );
        circle->line_width = atoi( (*arrayfirst)[3] );
        circle->center_x = atof( (*arrayfirst)[4] );
        circle->center_y = atof( (*arrayfirst)[5] );
        circle->radius = atof( (*arrayfirst)[6] );
        //save struct data ptr
        m_StructData = circle;
    }
	/** 円弧 **/
	else if(IN_Feature->EntityName == "arc_feature" ){//6
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName,  "ARC");
		strcpy_s(m_TypeName, _countof(m_TypeName),  "ARC");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 10)//syntax error
            return FALSE;
        //make up struct data
        Arc_Struct* arc = new Arc_Struct;
        arc->layer = atoi( (*arrayfirst)[0] );
        arc->color = atoi( (*arrayfirst)[1] );
        arc->type = atoi( (*arrayfirst)[2] );
        arc->line_width = atoi( (*arrayfirst)[3] );
        arc->center_x = atof( (*arrayfirst)[4] );
        arc->center_y = atof( (*arrayfirst)[5] );
        arc->radius = atof( (*arrayfirst)[6] );
        arc->direction = atoi( (*arrayfirst)[7] );
        arc->start_angle = atof( (*arrayfirst)[8] );
        arc->end_angle = atof( (*arrayfirst)[9] );
        //save struct data ptr
        m_StructData = arc;
    }
	/** 楕円 **/
	else if(IN_Feature->EntityName == "ellipse_feature" ){//7
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "ELLIPSE");
		strcpy_s(m_TypeName, _countof(m_TypeName), "ELLIPSE");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 9)//syntax error
            return FALSE;
        //make up struct data
        Ellipse_Struct* ellipse = new Ellipse_Struct;
        ellipse->layer = atoi( (*arrayfirst)[0] );
        ellipse->color = atoi( (*arrayfirst)[1] );
        ellipse->type = atoi( (*arrayfirst)[2] );
        ellipse->line_width = atoi( (*arrayfirst)[3] );
        ellipse->center_x = atof( (*arrayfirst)[4] );
        ellipse->center_y = atof( (*arrayfirst)[5] );
        ellipse->radius_x = atof( (*arrayfirst)[6] );
        ellipse->radius_y = atof( (*arrayfirst)[7] );
        ellipse->rotation_angle = atof( (*arrayfirst)[8] );
        //save struct data ptr
        m_StructData = ellipse;
    }
	/** 楕円弧 **/
	else if(IN_Feature->EntityName == "ellipse_arc_feature" ){//8
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "ELLIPSE_ARC");
		strcpy_s(m_TypeName, _countof(m_TypeName), "ELLIPSE_ARC");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 12)//syntax error
            return FALSE;
        //make up struct data
        Ellipse_Arc_Struct* ellipsearc = new Ellipse_Arc_Struct;
        ellipsearc->layer = atoi( (*arrayfirst)[0] );
        ellipsearc->color = atoi( (*arrayfirst)[1] );
        ellipsearc->type = atoi( (*arrayfirst)[2] );
        ellipsearc->line_width = atoi( (*arrayfirst)[3] );
        ellipsearc->center_x = atof( (*arrayfirst)[4] );
        ellipsearc->center_y = atof( (*arrayfirst)[5] );
        ellipsearc->radius_x = atof( (*arrayfirst)[6] );
        ellipsearc->radius_y = atof( (*arrayfirst)[7] );
        ellipsearc->direction = atoi( (*arrayfirst)[8] );
        ellipsearc->rotation_angle = atof( (*arrayfirst)[9] );
        ellipsearc->start_angle = atof( (*arrayfirst)[10] );
        ellipsearc->end_angle = atof( (*arrayfirst)[11] );
        //save struct data ptr
        m_StructData = ellipsearc;
    }
	/** 文字要素 **/
	else if(IN_Feature->EntityName == "text_string_feature" ){//9
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "TEXT");
		strcpy_s(m_TypeName, _countof(m_TypeName), "TEXT");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 13)//syntax error
            return FALSE;
        //make up struct data
        Text_Struct* textstring = new Text_Struct;
        textstring->layer = atoi( (*arrayfirst)[0] );
        textstring->color = atoi( (*arrayfirst)[1] );
        textstring->font = atoi( (*arrayfirst)[2] );
		/* 文字列の長さを調べる */
		ilen = strlen((*arrayfirst)[3]);
		//if (ilen <= 255){		bugfix	T.Matsushima	2015.04.27
		if (ilen <= 256){
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(textstring->str,(*arrayfirst)[3]);
			strcpy_s(textstring->str,_countof(textstring->str),(*arrayfirst)[3]);
		}else{
			/* メッセージを出力 */
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName);
			/* 先頭から256バイト切り取る */
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(textstring->str,(*arrayfirst)[3],256);
			strncpy_s(textstring->str,_countof(textstring->str),(*arrayfirst)[3],256);
			textstring->str[256] = 0x00;
		}
        textstring->text_x = atof( (*arrayfirst)[4] );
        textstring->text_y = atof( (*arrayfirst)[5] );
        textstring->height = atof( (*arrayfirst)[6] );
        textstring->width = atof( (*arrayfirst)[7] );
        textstring->spc = atof( (*arrayfirst)[8] );
        textstring->angle = atof( (*arrayfirst)[9] );
        textstring->slant = atof( (*arrayfirst)[10] );
        textstring->b_pnt = atoi( (*arrayfirst)[11] );
        textstring->direct = atoi( (*arrayfirst)[12] );
        //save struct data ptr
        m_StructData = textstring;
    }
	/** スプライン **/
	else if(IN_Feature->EntityName == "spline_feature" ){//10
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "SPLINE");
		strcpy_s(m_TypeName, _countof(m_TypeName), "SPLINE");
		//split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 8)//syntax error
            return FALSE;
        //make up struct data
        Spline_Struct* spline = new Spline_Struct;
        spline->layer = atoi( (*arrayfirst)[0] );
        spline->color = atoi( (*arrayfirst)[1] );
        spline->type = atoi( (*arrayfirst)[2] );
        spline->line_width = atoi( (*arrayfirst)[3] );
        spline->open_close = atoi( (*arrayfirst)[4] );
        spline->number = atoi( (*arrayfirst)[5] );
        //add x
        (*arrayfirst)[6].TrimLeft('(');
        (*arrayfirst)[6].TrimRight(')');
        ComSplitStringC splitsecond((*arrayfirst)[6]);
        arraysecond = splitsecond.Split(',');
        INT_PTR arraysecondsize = arraysecond->GetSize();
        if (arraysecondsize != spline->number )//syntax error
		{
			SXFErrorMsgC::RecordError(SXFVERTEXERRORSPLINE,
										m_TypeName,
										3,
										InsID,spline->number,
										m_TypeName);
			return FALSE;
		}
        for(int i=0;i<arraysecondsize;i++){
            spline->x.Add(atof((*arraysecond)[i]));
        }
        //add y
        (*arrayfirst)[7].TrimLeft('(');
        (*arrayfirst)[7].TrimRight(')');
        ComSplitStringC splitthird((*arrayfirst)[7]);
        arraythird = splitthird.Split(',');
        INT_PTR arraythirdsize = arraythird->GetSize();
        if (arraythirdsize != spline->number)//syntax error
            return FALSE;
        for(i=0;i<arraythirdsize;i++){
            spline->y.Add(atof((*arraythird)[i]));
        }
        //save struct data ptr
        m_StructData = spline;
    }
	/** クロソイド **/
	else if(IN_Feature->EntityName == "clothoid_feature" ){//10
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "CLOTHOID");
		strcpy_s(m_TypeName, _countof(m_TypeName), "CLOTHOID");
		//split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 11)//syntax error
            return FALSE;
        //make up struct data
        Clothoid_Struct* clothoid = new Clothoid_Struct;
        clothoid->layer = atoi( (*arrayfirst)[0] );
        clothoid->color = atoi( (*arrayfirst)[1] );
        clothoid->type = atoi( (*arrayfirst)[2] );
        clothoid->line_width = atoi( (*arrayfirst)[3] );
        clothoid->base_x = atof( (*arrayfirst)[4] );
        clothoid->base_y = atof( (*arrayfirst)[5] );
        clothoid->parameter = atof( (*arrayfirst)[6] );
        clothoid->direction = atoi( (*arrayfirst)[7] );
        clothoid->angle = atof( (*arrayfirst)[8] );
        clothoid->start_length = atof( (*arrayfirst)[9] );
        clothoid->end_length = atof( (*arrayfirst)[10] );
        //save struct data ptr
        m_StructData = clothoid;
    }
	/** 複合図形定義 **/
	else if(IN_Feature->EntityName == "sfig_org_feature" ){//11
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 2)//syntax error
            return FALSE;
        //make up struct data
        Sfigorg_Struct* sfigorg = new Sfigorg_Struct;
		/* 複合図形名の長さを調べる */
		ilen = strlen((*arrayfirst)[0]);
		//if (ilen <= 255){		bugfix	T.Matsushima	2015.04.27
		if (ilen <= 256){
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(sfigorg->name,(*arrayfirst)[0]);
			strcpy_s(sfigorg->name,_countof(sfigorg->name),(*arrayfirst)[0]);
		}else{
			/* メッセージを出力 */
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										"SFIG_ORG",
										2,
										InsID,
										"SFIG_ORG");
			/* 先頭から256バイト切り取る */
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(sfigorg->name,(*arrayfirst)[0],256);
			strncpy_s(sfigorg->name,_countof(sfigorg->name),(*arrayfirst)[0],256);
			sfigorg->name[256] = 0x00;
		}
        sfigorg->flag = atoi( (*arrayfirst)[1] );
        //save struct data ptr
        m_StructData = sfigorg;
    }
	/** 複合図形配置 **/
	else if(IN_Feature->EntityName == "sfig_locate_feature" ){//12
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "SFIG_LOCATE");
		strcpy_s(m_TypeName, _countof(m_TypeName), "SFIG_LOCATE");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 7)//syntax error
            return FALSE;
        //make up struct data
        Sfigloc_Struct* sfiglocate = new Sfigloc_Struct;
        sfiglocate->layer = atoi( (*arrayfirst)[0] );
		/* 複合図形名の長さを調べる */
		ilen = strlen((*arrayfirst)[1]);
		//if (ilen <= 255){		bugfix	T.Matsushima	2015.04.27
		if (ilen <= 256){
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(sfiglocate->name,(*arrayfirst)[1]);
			strcpy_s(sfiglocate->name,_countof(sfiglocate->name),(*arrayfirst)[1]);
		}else{
			/* メッセージを出力 */
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName);
			/* 先頭から256バイト切り取る */
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(sfiglocate->name,(*arrayfirst)[1],256);
			strncpy_s(sfiglocate->name,_countof(sfiglocate->name),(*arrayfirst)[1],256);
			sfiglocate->name[256] = 0x00;
		}
        sfiglocate->x = atof( (*arrayfirst)[2] );
        sfiglocate->y = atof( (*arrayfirst)[3] );
        sfiglocate->angle = atof( (*arrayfirst)[4] );
        sfiglocate->ratio_x = atof( (*arrayfirst)[5] );
        sfiglocate->ratio_y = atof( (*arrayfirst)[6] );
        //save struct data ptr
        m_StructData = sfiglocate;
    }
	/** 既定義シンボル **/
	else if(IN_Feature->EntityName == "externally_defined_symbol_feature" ){//13
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "EXTERNALLY_DEFINED_SYMBOL");
		strcpy_s(m_TypeName, _countof(m_TypeName), "EXTERNALLY_DEFINED_SYMBOL");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 8)//syntax error
            return FALSE;
        //make up struct data
        Externally_Defined_Symbol_Struct* symbol = new Externally_Defined_Symbol_Struct;
        symbol->layer = atoi( (*arrayfirst)[0] );
        symbol->color_flag = atoi( (*arrayfirst)[1] );
        symbol->color = atoi( (*arrayfirst)[2] );
		//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy( symbol->name,(*arrayfirst)[3] );

		// 既定義シンボル名
		if( (*arrayfirst)[3].GetLength() <= 256 ) {
			strcpy_s( symbol->name, _countof(symbol->name), (*arrayfirst)[3]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			strncpy_s( symbol->name, _countof(symbol->name),(*arrayfirst)[3],256) ;
			symbol->name[256] = 0x00 ;
		}
		//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
        symbol->start_x = atof( (*arrayfirst)[4] );
        symbol->start_y = atof( (*arrayfirst)[5] );
        symbol->rotate_angle = atof( (*arrayfirst)[6] );
        symbol->scale = atof( (*arrayfirst)[7] );
        //save struct data ptr
        m_StructData = symbol;
    }
	/** 直線寸法 **/
	else if(IN_Feature->EntityName == "linear_dim_feature" ){//14
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "LINEAR_DIMENSION");
		strcpy_s(m_TypeName, _countof(m_TypeName), "LINEAR_DIMENSION");
		//split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 44)//syntax error
            return FALSE;
        //make up struct data
        LinearDim_Struct* lineardim = new LinearDim_Struct;
        lineardim->layer = atoi( (*arrayfirst)[0] );
        lineardim->color = atoi( (*arrayfirst)[1] );
        lineardim->type = atoi( (*arrayfirst)[2] );
        lineardim->line_width = atoi( (*arrayfirst)[3] );
        lineardim->sun_x1 = atof( (*arrayfirst)[4] );
        lineardim->sun_y1 = atof( (*arrayfirst)[5] );
        lineardim->sun_x2 = atof( (*arrayfirst)[6] );
        lineardim->sun_y2 = atof( (*arrayfirst)[7] );
        lineardim->flg2 = atoi( (*arrayfirst)[8] );
        lineardim->ho1_x0 = atof( (*arrayfirst)[9] );
        lineardim->ho1_y0 = atof( (*arrayfirst)[10] );
        lineardim->ho1_x1 = atof( (*arrayfirst)[11] );
        lineardim->ho1_y1 = atof( (*arrayfirst)[12] );
        lineardim->ho1_x2 = atof( (*arrayfirst)[13] );
        lineardim->ho1_y2 = atof( (*arrayfirst)[14] );
        lineardim->flg3 = atoi( (*arrayfirst)[15] );
        lineardim->ho2_x0 = atof( (*arrayfirst)[16] );
        lineardim->ho2_y0 = atof( (*arrayfirst)[17] );
        lineardim->ho2_x1 = atof( (*arrayfirst)[18] );
        lineardim->ho2_y1 = atof( (*arrayfirst)[19] );
        lineardim->ho2_x2 = atof( (*arrayfirst)[20] );
        lineardim->ho2_y2 = atof( (*arrayfirst)[21] );
        lineardim->arr1_code1 = atoi( (*arrayfirst)[22] );
        lineardim->arr1_code2 = atoi( (*arrayfirst)[23] );
        lineardim->arr1_x = atof( (*arrayfirst)[24] );
        lineardim->arr1_y = atof( (*arrayfirst)[25] );
        lineardim->arr1_r = atof( (*arrayfirst)[26] );
        lineardim->arr2_code1 = atoi( (*arrayfirst)[27] );
        lineardim->arr2_code2 = atoi( (*arrayfirst)[28] );
        lineardim->arr2_x = atof( (*arrayfirst)[29] );
        lineardim->arr2_y = atof( (*arrayfirst)[30] );
        lineardim->arr2_r = atof( (*arrayfirst)[31] );
        lineardim->flg4 = atoi( (*arrayfirst)[32] );
        lineardim->font = atoi( (*arrayfirst)[33] );
		//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(lineardim->str,(*arrayfirst)[34]);

		// 直線寸法 文字列
		if( (*arrayfirst)[34].GetLength() <= 256 ) {
			strcpy_s( lineardim->str, _countof(lineardim->str), (*arrayfirst)[34]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			strncpy_s( lineardim->str, _countof(lineardim->str),(*arrayfirst)[34],256) ;
			lineardim->str[256] = 0x00 ;
		}
		//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
        lineardim->text_x = atof( (*arrayfirst)[35] );
        lineardim->text_y= atof( (*arrayfirst)[36] );
        lineardim->height = atof( (*arrayfirst)[37] );
        lineardim->width = atof( (*arrayfirst)[38] );
        lineardim->spc = atof( (*arrayfirst)[39] );
        lineardim->angle = atof( (*arrayfirst)[40] );
        lineardim->slant = atof( (*arrayfirst)[41] );
        lineardim->b_pnt = atoi( (*arrayfirst)[42] );
        lineardim->direct = atoi( (*arrayfirst)[43] );
        //save struct data ptr
        m_StructData = lineardim;
    }
	/** 弧長寸法 **/
	else if(IN_Feature->EntityName == "curve_dim_feature" ){//15
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "CURVE_DIMENSION");
		strcpy_s(m_TypeName, _countof(m_TypeName), "CURVE_DIMENSION");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 45)//syntax error
            return FALSE;
        //make up struct data
        CurveDim_Struct* curvedim = new CurveDim_Struct;
        curvedim->layer = atoi( (*arrayfirst)[0] );
        curvedim->color = atoi( (*arrayfirst)[1] );
        curvedim->type = atoi( (*arrayfirst)[2] );
        curvedim->line_width = atoi( (*arrayfirst)[3] );
        curvedim->sun_x = atof( (*arrayfirst)[4] );
        curvedim->sun_y = atof( (*arrayfirst)[5] );
        curvedim->sun_radius = atof( (*arrayfirst)[6] );
        curvedim->sun_angle0 = atof( (*arrayfirst)[7] );
        curvedim->sun_angle1 = atof( (*arrayfirst)[8] );
        curvedim->flg2 = atoi( (*arrayfirst)[9] );
        curvedim->ho1_x0 = atof( (*arrayfirst)[10] );
        curvedim->ho1_y0 = atof( (*arrayfirst)[11] );
        curvedim->ho1_x1 = atof( (*arrayfirst)[12] );
        curvedim->ho1_y1 = atof( (*arrayfirst)[13] );
        curvedim->ho1_x2 = atof( (*arrayfirst)[14] );
        curvedim->ho1_y2 = atof( (*arrayfirst)[15]);
        curvedim->flg3 = atoi(   (*arrayfirst)[16] );
        curvedim->ho2_x0 = atof( (*arrayfirst)[17] );
        curvedim->ho2_y0 = atof( (*arrayfirst)[18] );
        curvedim->ho2_x1 = atof( (*arrayfirst)[19] );
        curvedim->ho2_y1 = atof( (*arrayfirst)[20] );
        curvedim->ho2_x2 = atof( (*arrayfirst)[21] );
        curvedim->ho2_y2 = atof( (*arrayfirst)[22]);
        curvedim->arr1_code1 = atoi( (*arrayfirst)[23] );
        curvedim->arr1_code2 = atoi( (*arrayfirst)[24] );
        curvedim->arr1_x = atof( (*arrayfirst)[25] );
        curvedim->arr1_y = atof( (*arrayfirst)[26] );
        curvedim->arr1_r = atof( (*arrayfirst)[27] );
        curvedim->arr2_code1 = atoi( (*arrayfirst)[28] );
        curvedim->arr2_code2 = atoi( (*arrayfirst)[29] );
        curvedim->arr2_x = atof( (*arrayfirst)[30] );
        curvedim->arr2_y = atof( (*arrayfirst)[31] );
        curvedim->arr2_r = atof( (*arrayfirst)[32] );
        curvedim->flg4 = atoi( (*arrayfirst)[33] );
        curvedim->font = atoi( (*arrayfirst)[34] );
		//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(curvedim->str,(*arrayfirst)[35]);

		// 弧長寸法 文字列
		if( (*arrayfirst)[35].GetLength() <= 256 ) {
			strcpy_s( curvedim->str, _countof(curvedim->str), (*arrayfirst)[35]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			strncpy_s( curvedim->str, _countof(curvedim->str),(*arrayfirst)[35],256) ;
			curvedim->str[256] = 0x00 ;
		}
		//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
        curvedim->text_x = atof( (*arrayfirst)[36] );
        curvedim->text_y= atof( (*arrayfirst)[37] );
        curvedim->height = atof( (*arrayfirst)[38] );
        curvedim->width = atof( (*arrayfirst)[39] );
        curvedim->spc = atof( (*arrayfirst)[40] );
        curvedim->angle = atof( (*arrayfirst)[41] );
        curvedim->slant = atof( (*arrayfirst)[42] );
        curvedim->b_pnt = atoi( (*arrayfirst)[43] );
        curvedim->direct = atoi( (*arrayfirst)[44] );
        //save struct data ptr
        m_StructData = curvedim;
    }
	/** 角度寸法 **/
	else if(IN_Feature->EntityName == "angular_dim_feature" ){//15
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "ANGULAR_DIMENSION");
		strcpy_s(m_TypeName, _countof(m_TypeName), "ANGULAR_DIMENSION");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 45)//syntax error
            return FALSE;
        //make up struct data
        AngularDim_Struct* angulardim = new AngularDim_Struct;
        angulardim->layer = atoi( (*arrayfirst)[0] );
        angulardim->color = atoi( (*arrayfirst)[1] );
        angulardim->type = atoi( (*arrayfirst)[2] );
        angulardim->line_width = atoi( (*arrayfirst)[3] );
        angulardim->sun_x = atof( (*arrayfirst)[4] );
        angulardim->sun_y = atof( (*arrayfirst)[5] );
        angulardim->sun_radius = atof( (*arrayfirst)[6] );
        angulardim->sun_angle0 = atof( (*arrayfirst)[7] );
        angulardim->sun_angle1 = atof( (*arrayfirst)[8] );
        angulardim->flg2 = atoi( (*arrayfirst)[9] );
        angulardim->ho1_x0 = atof( (*arrayfirst)[10] );
        angulardim->ho1_y0 = atof( (*arrayfirst)[11] );
        angulardim->ho1_x1 = atof( (*arrayfirst)[12] );
        angulardim->ho1_y1 = atof( (*arrayfirst)[13] );
        angulardim->ho1_x2 = atof( (*arrayfirst)[14] );
        angulardim->ho1_y2 = atof( (*arrayfirst)[15]);
        angulardim->flg3 = atoi(   (*arrayfirst)[16] );
        angulardim->ho2_x0 = atof( (*arrayfirst)[17] );
        angulardim->ho2_y0 = atof( (*arrayfirst)[18] );
        angulardim->ho2_x1 = atof( (*arrayfirst)[19] );
        angulardim->ho2_y1 = atof( (*arrayfirst)[20] );
        angulardim->ho2_x2 = atof( (*arrayfirst)[21] );
        angulardim->ho2_y2 = atof( (*arrayfirst)[22]);
        angulardim->arr1_code1 = atoi( (*arrayfirst)[23] );
        angulardim->arr1_code2 = atoi( (*arrayfirst)[24] );
        angulardim->arr1_x = atof( (*arrayfirst)[25] );
        angulardim->arr1_y = atof( (*arrayfirst)[26] );
        angulardim->arr1_r = atof( (*arrayfirst)[27] );
        angulardim->arr2_code1 = atoi( (*arrayfirst)[28] );
        angulardim->arr2_code2 = atoi( (*arrayfirst)[29] );
        angulardim->arr2_x = atof( (*arrayfirst)[30] );
        angulardim->arr2_y = atof( (*arrayfirst)[31] );
        angulardim->arr2_r = atof( (*arrayfirst)[32] );
        angulardim->flg4 = atoi( (*arrayfirst)[33] );
        angulardim->font = atoi( (*arrayfirst)[34] );
		//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(angulardim->str,(*arrayfirst)[35]);

		// 角度寸法 文字列
		if( (*arrayfirst)[35].GetLength() <= 256 ) {
			strcpy_s( angulardim->str, _countof(angulardim->str), (*arrayfirst)[35]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			strncpy_s( angulardim->str, _countof(angulardim->str),(*arrayfirst)[35],256) ;
			angulardim->str[256] = 0x00 ;
		}
		//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
        angulardim->text_x = atof( (*arrayfirst)[36] );
        angulardim->text_y= atof( (*arrayfirst)[37] );
        angulardim->height = atof( (*arrayfirst)[38] );
        angulardim->width = atof( (*arrayfirst)[39] );
        angulardim->spc = atof( (*arrayfirst)[40] );
        angulardim->angle = atof( (*arrayfirst)[41] );
        angulardim->slant = atof( (*arrayfirst)[42] );
        angulardim->b_pnt = atoi( (*arrayfirst)[43] );
        angulardim->direct = atoi( (*arrayfirst)[44] );
        //save struct data ptr
        m_StructData = angulardim;
    }
	/** 半径寸法 **/
	else if(IN_Feature->EntityName == "radius_dim_feature" ){//16
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName,  "RADIUS_DIMENSION");
		strcpy_s(m_TypeName, _countof(m_TypeName), "RADIUS_DIMENSION");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 25)//syntax error
            return FALSE;
        //make up struct data
        RadiusDim_Struct* radiusdim = new RadiusDim_Struct;
        radiusdim->layer = atoi( (*arrayfirst)[0] );
        radiusdim->color = atoi( (*arrayfirst)[1] );
        radiusdim->type = atoi( (*arrayfirst)[2] );
        radiusdim->line_width = atoi( (*arrayfirst)[3] );
        radiusdim->sun_x1 = atof( (*arrayfirst)[4] );
        radiusdim->sun_y1 = atof( (*arrayfirst)[5] );
        radiusdim->sun_x2 = atof( (*arrayfirst)[6] );
        radiusdim->sun_y2 = atof( (*arrayfirst)[7] );
        radiusdim->arr_code1 = atoi( (*arrayfirst)[8] );
        radiusdim->arr_code2 = atoi( (*arrayfirst)[9] );
        radiusdim->arr_x = atof( (*arrayfirst)[10] );
        radiusdim->arr_y = atof( (*arrayfirst)[11] );
        radiusdim->arr_r = atof( (*arrayfirst)[12] );
        radiusdim->flg = atoi( (*arrayfirst)[13] );
        radiusdim->font = atoi( (*arrayfirst)[14] );
		//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(radiusdim->str,(*arrayfirst)[15]);

		// 半径寸法 文字列
		if( (*arrayfirst)[15].GetLength() <= 256 ) {
			strcpy_s( radiusdim->str, _countof(radiusdim->str), (*arrayfirst)[15]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			strncpy_s( radiusdim->str, _countof(radiusdim->str),(*arrayfirst)[15],256) ;
			radiusdim->str[256] = 0x00 ;
		}
		//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
        radiusdim->text_x = atof(   (*arrayfirst)[16] );
        radiusdim->text_y = atof( (*arrayfirst)[17] );
        radiusdim->height = atof( (*arrayfirst)[18] );
        radiusdim->width  = atof( (*arrayfirst)[19] );
        radiusdim->spc = atof( (*arrayfirst)[20] );
        radiusdim->angle = atof( (*arrayfirst)[21] );
        radiusdim->slant = atof( (*arrayfirst)[22] );
        radiusdim->b_pnt = atoi( (*arrayfirst)[23] );
        radiusdim->direct = atoi( (*arrayfirst)[24] );
        //save struct data ptr
        m_StructData = radiusdim;
    }
	/** 直径寸法 **/
	else if(IN_Feature->EntityName == "diameter_dim_feature" ){//17
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "DIAMETER_DIMENSION");
		strcpy_s(m_TypeName, _countof(m_TypeName), "DIAMETER_DIMENSION");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 30)//syntax error
            return FALSE;
        //make up struct data
        DiameterDim_Struct* diameterdim = new DiameterDim_Struct;
        diameterdim->layer = atoi( (*arrayfirst)[0] );
        diameterdim->color = atoi( (*arrayfirst)[1] );
        diameterdim->type = atoi( (*arrayfirst)[2] );
        diameterdim->line_width = atoi( (*arrayfirst)[3] );
        diameterdim->sun_x1 = atof( (*arrayfirst)[4] );
        diameterdim->sun_y1 = atof( (*arrayfirst)[5] );
        diameterdim->sun_x2 = atof( (*arrayfirst)[6] );
        diameterdim->sun_y2 = atof( (*arrayfirst)[7] );
        diameterdim->arr1_code1 = atoi( (*arrayfirst)[8] );
        diameterdim->arr1_code2 = atoi( (*arrayfirst)[9] );
        diameterdim->arr1_x = atof( (*arrayfirst)[10] );
        diameterdim->arr1_y = atof( (*arrayfirst)[11] );
        diameterdim->arr1_r = atof( (*arrayfirst)[12] );
        diameterdim->arr2_code1 = atoi( (*arrayfirst)[13] );
        diameterdim->arr2_code2 = atoi( (*arrayfirst)[14] );
        diameterdim->arr2_x = atof( (*arrayfirst)[15] );
        diameterdim->arr2_y = atof( (*arrayfirst)[16] );
        diameterdim->arr2_r = atof( (*arrayfirst)[17] );
        diameterdim->flg = atoi( (*arrayfirst)[18] );
        diameterdim->font = atoi( (*arrayfirst)[19] );
		//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(diameterdim->str,(*arrayfirst)[20]);

		// 直径寸法 文字列
		if( (*arrayfirst)[20].GetLength() <= 256 ) {
			strcpy_s( diameterdim->str, _countof(diameterdim->str), (*arrayfirst)[20]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			strncpy_s( diameterdim->str, _countof(diameterdim->str),(*arrayfirst)[20],256) ;
			diameterdim->str[256] = 0x00 ;
		}
		//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
        diameterdim->text_x = atof(   (*arrayfirst)[21] );
        diameterdim->text_y = atof( (*arrayfirst)[22] );
        diameterdim->height = atof( (*arrayfirst)[23] );
        diameterdim->width  = atof( (*arrayfirst)[24] );
        diameterdim->spc = atof( (*arrayfirst)[25] );
        diameterdim->angle = atof( (*arrayfirst)[26] );
        diameterdim->slant = atof( (*arrayfirst)[27] );
        diameterdim->b_pnt = atoi( (*arrayfirst)[28] );
        diameterdim->direct = atoi( (*arrayfirst)[29] );
        //save struct data ptr
        m_StructData = diameterdim;
    }
	/** 引出し線 **/
	else if(IN_Feature->EntityName == "label_feature" ){//18
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "LABEL");
		strcpy_s(m_TypeName, _countof(m_TypeName), "LABEL");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 21)//syntax error
            return FALSE;
        //make up struct data
        Label_Struct* label = new Label_Struct;
        label->layer = atoi( (*arrayfirst)[0] );
        label->color = atoi( (*arrayfirst)[1] );
        label->type = atoi( (*arrayfirst)[2] );
        label->line_width = atoi( (*arrayfirst)[3] );
        label->vertex_number = atoi( (*arrayfirst)[4] );
        //add x
        (*arrayfirst)[5].TrimLeft('(');
        (*arrayfirst)[5].TrimRight(')');
        ComSplitStringC splitsecond((*arrayfirst)[5]);
        arraysecond = splitsecond.Split(',');
        INT_PTR arraysecondsize = arraysecond->GetSize();
        if (arraysecondsize != label->vertex_number)//syntax error
		{
			SXFErrorMsgC::RecordError(SXFNOTVERTEXNUMBER,
										m_TypeName,
										3,
										InsID,
										label->vertex_number,
										m_TypeName);
			return FALSE;
		}
        for(int i=0;i<arraysecondsize;i++){
            label->vertex_x.Add(atof((*arraysecond)[i]));
        }
        //add y
        (*arrayfirst)[6].TrimLeft('(');
        (*arrayfirst)[6].TrimRight(')');
        ComSplitStringC splitthird((*arrayfirst)[6]);
        arraythird = splitthird.Split(',');
        INT_PTR arraythirdsize = arraythird->GetSize();
        if (arraythirdsize != label->vertex_number)//syntax error
		{
			SXFErrorMsgC::RecordError(SXFNOTVERTEXNUMBER,
										m_TypeName,
										3,
										InsID,
										label->vertex_number,
										m_TypeName);
			return FALSE;
		}
        for(i=0;i<arraythirdsize;i++){
            label->vertex_y.Add(atof((*arraythird)[i]));
        }
        label->arr_code = atoi( (*arrayfirst)[7] );
        label->arr_r = atof( (*arrayfirst)[8] );
        label->flg = atoi( (*arrayfirst)[9] );
        label->font = atoi( (*arrayfirst)[10] );
		//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(label->str,(*arrayfirst)[11]);

		// 引出し線 文字列
		if( (*arrayfirst)[11].GetLength() <= 256 ) {
			strcpy_s( label->str, _countof(label->str), (*arrayfirst)[11]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			strncpy_s( label->str, _countof(label->str),(*arrayfirst)[11],256) ;
			label->str[256] = 0x00 ;
		}
		//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
        label->text_x = atof(   (*arrayfirst)[12] );
        label->text_y = atof( (*arrayfirst)[13] );
        label->height = atof( (*arrayfirst)[14] );
        label->width  = atof( (*arrayfirst)[15] );
        label->spc = atof( (*arrayfirst)[16] );
        label->angle = atof( (*arrayfirst)[17] );
        label->slant = atof( (*arrayfirst)[18] );
        label->b_pnt = atoi( (*arrayfirst)[19] );
        label->direct = atoi( (*arrayfirst)[20] );
        //save struct data ptr
        m_StructData = label;
    }
	/** バルーン **/
	else if(IN_Feature->EntityName == "balloon_feature" ){//19
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName,  "BALLOON");
		strcpy_s(m_TypeName, _countof(m_TypeName),  "BALLOON");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 24)//syntax error
            return FALSE;
        //make up struct data
        Balloon_Struct* balloon = new Balloon_Struct;
        balloon->layer = atoi( (*arrayfirst)[0] );
        balloon->color = atoi( (*arrayfirst)[1] );
        balloon->type = atoi( (*arrayfirst)[2] );
        balloon->line_width = atoi( (*arrayfirst)[3] );
        balloon->vertex_number = atoi( (*arrayfirst)[4] );
        //add x
        (*arrayfirst)[5].TrimLeft('(');
        (*arrayfirst)[5].TrimRight(')');
        ComSplitStringC splitsecond((*arrayfirst)[5]);
        arraysecond = splitsecond.Split(',');
        INT_PTR arraysecondsize = arraysecond->GetSize();
        if (arraysecondsize != balloon->vertex_number)//syntax error
		{
			SXFErrorMsgC::RecordError(SXFNOTVERTEXNUMBER,
										m_TypeName,
										3,
										InsID,balloon->vertex_number,
										m_TypeName);
			return FALSE;
		}
        for(int i=0;i<arraysecondsize;i++){
            balloon->vertex_x.Add(atof((*arraysecond)[i]));
        }
        //add y
        (*arrayfirst)[6].TrimLeft('(');
        (*arrayfirst)[6].TrimRight(')');
        ComSplitStringC splitthird((*arrayfirst)[6]);
        arraythird = splitthird.Split(',');
        INT_PTR arraythirdsize = arraythird->GetSize();
        if (arraythirdsize != balloon->vertex_number)//syntax error
		{
			SXFErrorMsgC::RecordError(SXFNOTVERTEXNUMBER,
										m_TypeName,
										3,
										InsID,
										balloon->vertex_number,
										m_TypeName);
			return FALSE;
		}
        for(i=0;i<arraythirdsize;i++){
            balloon->vertex_y.Add(atof((*arraythird)[i]));
        }
        balloon->center_x = atof( (*arrayfirst)[7] );
        balloon->center_y = atof( (*arrayfirst)[8] );
        balloon->radius = atof( (*arrayfirst)[9] );
        balloon->arr_code = atoi( (*arrayfirst)[10] );
        balloon->arr_r = atof( (*arrayfirst)[11] );
        balloon->flg = atoi( (*arrayfirst)[12] );
        balloon->font = atoi( (*arrayfirst)[13] );
		//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(balloon->str,(*arrayfirst)[14]);

		// バルーン 文字列
		if( (*arrayfirst)[14].GetLength() <= 256 ) {
			strcpy_s( balloon->str, _countof(balloon->str), (*arrayfirst)[14]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			strncpy_s( balloon->str, _countof(balloon->str),(*arrayfirst)[14],256) ;
			balloon->str[256] = 0x00 ;
		}
		//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
        balloon->text_x = atof(   (*arrayfirst)[15] );
        balloon->text_y = atof( (*arrayfirst)[16] );
        balloon->height = atof( (*arrayfirst)[17] );
        balloon->width  = atof( (*arrayfirst)[18] );
        balloon->spc = atof( (*arrayfirst)[19] );
        balloon->angle = atof( (*arrayfirst)[20] );
        balloon->slant = atof( (*arrayfirst)[21] );
        balloon->b_pnt = atoi( (*arrayfirst)[22] );
        balloon->direct = atoi( (*arrayfirst)[23] );
        //save struct data ptr
        m_StructData = balloon;
    }
	/** 外部定義ハッチング **/
	else if(IN_Feature->EntityName == "externally_defined_hatch_feature" ){//20
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "EXTERNALLY_DEFINED_HATCH");
		strcpy_s(m_TypeName, _countof(m_TypeName), "EXTERNALLY_DEFINED_HATCH");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 5)//syntax error
            return FALSE;
        //make up struct data
        Externally_Defined_Hatch_Struct* externallyhatch = new Externally_Defined_Hatch_Struct;
        externallyhatch->layer = atoi( (*arrayfirst)[0] );
		//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(externallyhatch->name, (*arrayfirst)[1] );

		// 外部定義ハッチング名
		if( (*arrayfirst)[1].GetLength() <= 256 ) {
			strcpy_s( externallyhatch->name, _countof(externallyhatch->name), (*arrayfirst)[1]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			strncpy_s( externallyhatch->name, _countof(externallyhatch->name),(*arrayfirst)[1],256) ;
			externallyhatch->name[256] = 0x00 ;
		}
		//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
        externallyhatch->out_id = atoi( (*arrayfirst)[2] );
        externallyhatch->number = atoi( (*arrayfirst)[3] );
        //add 
        (*arrayfirst)[4].TrimLeft('(');
        (*arrayfirst)[4].TrimRight(')');
        ComSplitStringC splitsecond((*arrayfirst)[4]);
        arraysecond = splitsecond.Split(',');
        INT_PTR arraysecondsize = arraysecond->GetSize();
        for(int i=0;i<arraysecondsize && i<externallyhatch->number;i++){
            externallyhatch->in_id.Add( atoi( (*arraysecond)[i] ) );
        }
        //save struct data ptr
        m_StructData = externallyhatch;
    }
	/** ハッチング(塗り) **/
	else if(IN_Feature->EntityName == "fill_area_style_colour_feature" ){//21
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName,  "FILL_AREA_STYLE_COLOUR");
		strcpy_s(m_TypeName, _countof(m_TypeName), "FILL_AREA_STYLE_COLOUR");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 5)//syntax error
            return FALSE;
        //make up struct data
        Fill_area_style_colour_Struct* fillcolor = new Fill_area_style_colour_Struct;
        fillcolor->layer = atoi( (*arrayfirst)[0] );
        fillcolor->color = atoi( (*arrayfirst)[1] );
        fillcolor->out_id = atoi( (*arrayfirst)[2] );
        fillcolor->number = atoi( (*arrayfirst)[3] );
        //add 
        (*arrayfirst)[4].TrimLeft('(');
        (*arrayfirst)[4].TrimRight(')');
        ComSplitStringC splitsecond((*arrayfirst)[4]);
        arraysecond = splitsecond.Split(',');
        INT_PTR arraysecondsize = arraysecond->GetSize();
        for(int i=0;i<arraysecondsize && i<fillcolor->number;i++){
            fillcolor->in_id.Add( atoi( (*arraysecond)[i] ) );
        }
        //save struct data ptr
        m_StructData = fillcolor;
    }
	/** ハッチング(ユーザ定義) **/
	else if(IN_Feature->EntityName == "fill_area_style_hatching_feature" ){//22
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "FILL_AREA_STYLE_HATCHING");
		strcpy_s(m_TypeName, _countof(m_TypeName), "FILL_AREA_STYLE_HATCHING");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        INT_PTR arrayfirstsize=arrayfirst->GetSize();
        if(arrayfirstsize < 6 || arrayfirstsize>9 )//syntax error
            return FALSE;
        //make up struct data
        Fill_area_style_hatching_Struct* fillstyle = new Fill_area_style_hatching_Struct;
        fillstyle->layer = atoi( (*arrayfirst)[0] );
        fillstyle->hatch_number = atoi( (*arrayfirst)[1] );
		/* ﾊｯﾁﾊﾟﾀｰﾝ数が範囲外の値 */
        if( fillstyle->hatch_number < 1 || fillstyle->hatch_number >4){
			SXFErrorMsgC::RecordError(SXFERRORPATTERN,
									"FILL_AREA_STYLE_HATCHING",
									3,
									InsID,
									fillstyle->hatch_number,
									"FILL_AREA_STYLE_HATCHING");
            return FALSE;//syntax error
		}
        if( fillstyle->hatch_number + 5 != arrayfirstsize){
			SXFErrorMsgC::RecordError(SXFERRORPATTERN,
									"FILL_AREA_STYLE_HATCHING",
									3,
									InsID,
									fillstyle->hatch_number,
									"FILL_AREA_STYLE_HATCHING");
            return FALSE;//syntax error
		}
        for(int i=0;i<fillstyle->hatch_number;i++){
            (*arrayfirst)[i+2].TrimLeft('(');
            (*arrayfirst)[i+2].TrimRight(')');
            ComSplitStringC splitsecond((*arrayfirst)[i+2]);
            arraysecond = splitsecond.Split(',');
            INT_PTR arraysecondsize = arraysecond->GetSize();
            if (arraysecondsize != 7)//syntax error
                return FALSE;
            fillstyle->hatch_color[i] = atoi( (*arraysecond)[0] ) ;
            fillstyle->hatch_type[i] = atoi( (*arraysecond)[1] ) ;
            fillstyle->hatch_line_width[i] = atoi( (*arraysecond)[2] ) ;
            fillstyle->hatch_start_x[i] = atof( (*arraysecond)[3] ) ;
            fillstyle->hatch_start_y[i] = atof( (*arraysecond)[4] ) ;
            fillstyle->hatch_spacing[i] = atof( (*arraysecond)[5] ) ;
            fillstyle->hatch_angle[i] = atof( (*arraysecond)[6] ) ;
        }
        fillstyle->out_id = atoi( (*arrayfirst)[arrayfirstsize-3] );
        fillstyle->number = atoi( (*arrayfirst)[arrayfirstsize-2] );
        //add in_id 
        (*arrayfirst)[arrayfirstsize-1].TrimLeft('(');
        (*arrayfirst)[arrayfirstsize-1].TrimRight(')');
        ComSplitStringC splitthird((*arrayfirst)[arrayfirstsize-1]);
        CStringArray* arraythird = splitthird.Split(',');
        INT_PTR arraythirdsize = arraythird->GetSize();
        for(i=0;i<arraythirdsize && i<fillstyle->number;i++){
            fillstyle->in_id.Add( atoi( (*arraythird)[i] ) ) ;
        }
        //save struct data ptr
        m_StructData = fillstyle;
    }
	/** ハッチング(パターン) **/
	else if(IN_Feature->EntityName == "fill_area_style_tiles_feature" ){//23
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "FILL_AREA_STYLE_TILES");
		strcpy_s(m_TypeName, _countof(m_TypeName), "FILL_AREA_STYLE_TILES");
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 15)//syntax error
            return FALSE;
        //make up struct data
        Fill_area_style_tiles_Struct* filltiles = new Fill_area_style_tiles_Struct;
        filltiles->layer = atoi( (*arrayfirst)[0] );
		//	MOD(S)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy( filltiles->name, (*arrayfirst)[1] );

		// ハッチング(パターン)名
		if( (*arrayfirst)[1].GetLength() <= 256 ) {
			strcpy_s( filltiles->name, _countof(filltiles->name), (*arrayfirst)[1]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			strncpy_s( filltiles->name, _countof(filltiles->name),(*arrayfirst)[1],256) ;
			filltiles->name[256] = 0x00 ;
		}
		//	MOD(E)	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
        filltiles->hatch_color = atoi( (*arrayfirst)[2] );
        filltiles->hatch_pattern_x = atof( (*arrayfirst)[3] );
        filltiles->hatch_pattern_y = atof( (*arrayfirst)[4] );
        filltiles->hatch_pattern_vector1 = atof( (*arrayfirst)[5] );
        filltiles->hatch_pattern_vector1_angle = atof( (*arrayfirst)[6] );
        filltiles->hatch_pattern_vector2 = atof( (*arrayfirst)[7] );
        filltiles->hatch_pattern_vector2_angle = atof( (*arrayfirst)[8] );
        filltiles->hatch_pattern_scale_x = atof( (*arrayfirst)[9] );
        filltiles->hatch_pattern_scale_y = atof( (*arrayfirst)[10] );
        filltiles->hatch_pattern_angle = atof( (*arrayfirst)[11] );
        filltiles->out_id = atoi( (*arrayfirst)[12] );
        filltiles->number = atoi( (*arrayfirst)[13] );
        //add 
        (*arrayfirst)[14].TrimLeft('(');
        (*arrayfirst)[14].TrimRight(')');
        ComSplitStringC splitsecond((*arrayfirst)[14]);
        arraysecond = splitsecond.Split(',');
        INT_PTR arraysecondsize = arraysecond->GetSize();
        for(int i=0;i<arraysecondsize && i<filltiles->number;i++){
            filltiles->in_id.Add( atoi( (*arraysecond)[i] ) );
        }
        //save struct data ptr
        m_StructData = filltiles;
    }
	/** 複合曲線定義 **/
	else if(IN_Feature->EntityName == "composite_curve_org_feature" ){//24
        //split feature data
        SXFSplitFeatureC splitfeature(IN_Feature->EntityData);
        arrayfirst = splitfeature.Split();
        if(arrayfirst->GetSize() != 4)//syntax error
            return FALSE;
        //make up struct data
        Ccurve_Org_Struct* curveorg = new Ccurve_Org_Struct;
        curveorg->color = atoi( (*arrayfirst)[0] );
        curveorg->type = atoi( (*arrayfirst)[1] );
        curveorg->line_width = atoi( (*arrayfirst)[2] );
        curveorg->flag = atoi( (*arrayfirst)[3] );
        //save struct data ptr
        m_StructData = curveorg;

//	ADD(S)	Ver3.0対応	K.Naono	02.12.03
	/** 図面表題欄 **/
	} else if(IN_Feature->EntityName == "drawing_attribute_feature" ){//25
		//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		//strcpy(m_TypeName, "ATTRIBUTE") ;
		strcpy_s(m_TypeName, _countof(m_TypeName), "ATTRIBUTE") ;
		//split feature data
		SXFSplitFeatureC	splitfeature(IN_Feature->EntityData) ;
		arrayfirst = splitfeature.Split() ;
		if( arrayfirst == NULL || arrayfirst->GetSize() != 12 )//syntax error
			return FALSE ;

		//make up struct data
		Attribute_Struct*	attr = new Attribute_Struct ;

		//	事業名の設定
		if( (*arrayfirst)[0].GetLength() <= 256 ) {
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(attr->p_name,(*arrayfirst)[0]) ;
			strcpy_s(attr->p_name,_countof(attr->p_name),(*arrayfirst)[0]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(attr->p_name,(*arrayfirst)[0],256) ;
			strncpy_s(attr->p_name,_countof(attr->p_name),(*arrayfirst)[0],256) ;
			attr->p_name[256] = 0x00 ;
		}

		//	工事名の設定
		if( (*arrayfirst)[1].GetLength() <= 256 ) {
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(attr->c_name,(*arrayfirst)[1]) ;
			strcpy_s(attr->c_name,_countof(attr->c_name),(*arrayfirst)[1]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(attr->c_name,(*arrayfirst)[1],256) ;
			strncpy_s(attr->c_name,_countof(attr->c_name),(*arrayfirst)[1],256) ;
			attr->c_name[256] = 0x00 ;
		}

		//	契約区分の設定
		if( (*arrayfirst)[2].GetLength() <= 256 ) {
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(attr->c_type,(*arrayfirst)[2]) ;
			strcpy_s(attr->c_type,_countof(attr->c_type),(*arrayfirst)[2]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(attr->c_type,(*arrayfirst)[2],256) ;
			strncpy_s(attr->c_type,_countof(attr->c_type),(*arrayfirst)[2],256) ;
			attr->c_type[256] = 0x00 ;
		}

		//	図面名の設定
		if( (*arrayfirst)[3].GetLength() <= 256 ) {
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(attr->d_title,(*arrayfirst)[3]) ;
			strcpy_s(attr->d_title,_countof(attr->d_title),(*arrayfirst)[3]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(attr->d_title,(*arrayfirst)[3],256) ;
			strncpy_s(attr->d_title,_countof(attr->d_title),(*arrayfirst)[3],256) ;
			attr->d_title[256] = 0x00 ;
		}

		//	図面番号の設定
		if( (*arrayfirst)[4].GetLength() <= 256 ) {
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(attr->d_number,(*arrayfirst)[4]) ;
			strcpy_s(attr->d_number,_countof(attr->d_number),(*arrayfirst)[4]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(attr->d_number,(*arrayfirst)[4],256) ;
			strncpy_s(attr->d_number,_countof(attr->d_number),(*arrayfirst)[4],256) ;
			attr->d_number[256] = 0x00 ;
		}

		//	図面種別の設定
		if( (*arrayfirst)[5].GetLength() <= 256 ) {
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(attr->d_type,(*arrayfirst)[5]) ;
			strcpy_s(attr->d_type,_countof(attr->d_type),(*arrayfirst)[5]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(attr->d_type,(*arrayfirst)[5],256) ;
			strncpy_s(attr->d_type,_countof(attr->d_type),(*arrayfirst)[5],256) ;
			attr->d_type[256] = 0x00 ;
		}

		//	尺度の設定
		if( (*arrayfirst)[6].GetLength() <= 256 ) {
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(attr->d_scale,(*arrayfirst)[6]) ;
			strcpy_s(attr->d_scale,_countof(attr->d_scale),(*arrayfirst)[6]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(attr->d_scale,(*arrayfirst)[6],256) ;
			strncpy_s(attr->d_scale,_countof(attr->d_scale),(*arrayfirst)[6],256) ;
			attr->d_scale[256] = 0x00 ;
		}

		// 図面作成年(西暦)
		attr->d_year	= atoi((*arrayfirst)[7]) ;

		// 図面作成月(西暦)
		attr->d_month	= atoi((*arrayfirst)[8]) ;

		// 図面作成日(西暦)
		attr->d_day	= atoi((*arrayfirst)[9]) ;

		//	受注会社名の設定
		if( (*arrayfirst)[10].GetLength() <= 256 ) {
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(attr->c_contractor,(*arrayfirst)[10]) ;
			strcpy_s(attr->c_contractor,_countof(attr->c_contractor),(*arrayfirst)[10]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(attr->c_contractor,(*arrayfirst)[10],256) ;
			strncpy_s(attr->c_contractor,_countof(attr->c_contractor),(*arrayfirst)[10],256) ;
			attr->c_contractor[256] = 0x00 ;
		}

		//	発注事業者名の設定
		if( (*arrayfirst)[11].GetLength() <= 256 ) {
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(attr->c_owner,(*arrayfirst)[11]) ;
			strcpy_s(attr->c_owner,_countof(attr->c_owner),(*arrayfirst)[11]) ;
		} else {
			// メッセージを出力
			SXFErrorMsgC::RecordError(SXFNAMELENGTH,
										m_TypeName,
										2,
										InsID,
										m_TypeName) ;
			// 先頭から256バイト切り取る
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strncpy(attr->c_owner,(*arrayfirst)[11],256) ;
			strncpy_s(attr->c_owner,_countof(attr->c_owner),(*arrayfirst)[11],256) ;
			attr->c_owner[256] = 0x00 ;
		}
		//save struct data ptr
		m_StructData = attr ;
//	ADD(E)	Ver3.0対応	K.Naono	02.12.03

    }else{//feature comment name error
		m_StructData = NULL;
        return FALSE;
    }
    return TRUE;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.04.17
//
//  １．機能概要
//      
//      Feature Comment data から構造体に変換する(WRITE)
//
//  ２．インターフェース
//      vpod SetFeatureData(LPCTSTR IN_TypeName, LPVOID IN_Struct)
//
//	    引数: 型			  引数名			    内容
//            LPCTSTR         IN_TypeName          構造体のTypeName
//            LPVOID          IN_Struct            構造体のアドレス
//
//	    復帰値	
//            なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.17     周
//		更新		2002.02.20		K.Naono	有効桁数15桁対応
//
//------------------------------------------------------------
void SXFFeatureC::SetFeatureData(LPCTSTR IN_TypeName, LPVOID IN_Struct)
{
	CString rot_ang,s_ang,e_ang,slt_ang;

	/* インスタンスモード */
	m_FeatureData.InstanceMode = SXF_IM_FT_ENTITY;
	/* インスタンスID */
	m_FeatureData.InstanceID = m_InstanceIDManager->GetNewInstanceID();
	/** 点マーカ **/ 
	if( strcmp(IN_TypeName, "POINT_MARKER") == 0 ){//2
		/* 出力フォーマット作成 */
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((Point_Marker_Struct*)IN_Struct)->rotate_angle);
        m_FeatureData.EntityName = "point_marker_feature";
        m_FeatureData.EntityData.Format(
//			"('%d','%d','%f','%f','%d','%s','%f')",
			"('%d','%d','%f','%f','%d','%s','%s')",
            ((Point_Marker_Struct*)IN_Struct)->layer,
            ((Point_Marker_Struct*)IN_Struct)->color,
            ((Point_Marker_Struct*)IN_Struct)->start_x,
            ((Point_Marker_Struct*)IN_Struct)->start_y,
            ((Point_Marker_Struct*)IN_Struct)->marker_code,
			rot_ang,
//			((Point_Marker_Struct*)IN_Struct)->scale
			SXFutyFunctionC::out_ctl(((Point_Marker_Struct*)IN_Struct)->scale)
            );
    }
	/** 線分 **/
	else if(strcmp(IN_TypeName, "LINE")==0 ){//3
		/* 出力フォーマット作成 */
        m_FeatureData.EntityName = "line_feature";
        m_FeatureData.EntityData.Format("('%d','%d','%d','%d','%f','%f','%f','%f')",
            ((Line_Struct*)IN_Struct)->layer,
            ((Line_Struct*)IN_Struct)->color,
            ((Line_Struct*)IN_Struct)->type,
            ((Line_Struct*)IN_Struct)->line_width,
            ((Line_Struct*)IN_Struct)->start_x,
            ((Line_Struct*)IN_Struct)->start_y,
            ((Line_Struct*)IN_Struct)->end_x,
            ((Line_Struct*)IN_Struct)->end_y
            );
    }
	/** 折線 **/
	else if(strcmp(IN_TypeName, "POLYLINE") == 0 ){//4
		/* 出力フォーマット作成 */
        m_FeatureData.EntityName = "polyline_feature";
        CString xstring;
        CString tempstring;
        for(int i=0;i<( ((Polyline_Struct*)IN_Struct)->number );i++){
            if(i!=0) xstring += ",";
            tempstring.Format("%f",((Polyline_Struct*)IN_Struct)->x[i]);
            xstring += tempstring;
        }
        CString ystring;
        for(i=0;i<( ((Polyline_Struct*)IN_Struct)->number );i++){
            if(i!=0) ystring += ",";
            tempstring.Format("%f",((Polyline_Struct*)IN_Struct)->y[i]);
            ystring += tempstring;
        }
        m_FeatureData.EntityData.Format("('%d','%d','%d','%d','%d','(%s)','(%s)')",
            ((Polyline_Struct*)IN_Struct)->layer,
            ((Polyline_Struct*)IN_Struct)->color,
            ((Polyline_Struct*)IN_Struct)->type,
            ((Polyline_Struct*)IN_Struct)->line_width,
            ((Polyline_Struct*)IN_Struct)->number,
            xstring,
            ystring
            );
    }
	/** 円 **/
	else if( strcmp(IN_TypeName, "CIRCLE") == 0 ){//5
		/* 出力フォーマット作成 */
        m_FeatureData.EntityName = "circle_feature";
        m_FeatureData.EntityData.Format("('%d','%d','%d','%d','%f','%f','%f')",
            ((Circle_Struct*)IN_Struct)->layer,
            ((Circle_Struct*)IN_Struct)->color,
            ((Circle_Struct*)IN_Struct)->type,
            ((Circle_Struct*)IN_Struct)->line_width,
            ((Circle_Struct*)IN_Struct)->center_x,
            ((Circle_Struct*)IN_Struct)->center_y,
            ((Circle_Struct*)IN_Struct)->radius
            );
    }
	/** 円弧 **/
	else if( strcmp( IN_TypeName,  "ARC") == 0 ){//6
		/* 出力フォーマット作成 */
        /* 角度の出力桁を15桁にする */
		s_ang = SXFutyFunctionC::out_ctl(((Arc_Struct*)IN_Struct)->start_angle);
		e_ang = SXFutyFunctionC::out_ctl(((Arc_Struct*)IN_Struct)->end_angle);
        m_FeatureData.EntityName = "arc_feature";
        m_FeatureData.EntityData.Format(
            "('%d','%d','%d','%d','%f','%f','%f','%d','%s','%s')",
            ((Arc_Struct*)IN_Struct)->layer,
            ((Arc_Struct*)IN_Struct)->color,
            ((Arc_Struct*)IN_Struct)->type,
            ((Arc_Struct*)IN_Struct)->line_width,
            ((Arc_Struct*)IN_Struct)->center_x,
            ((Arc_Struct*)IN_Struct)->center_y,
            ((Arc_Struct*)IN_Struct)->radius,
            ((Arc_Struct*)IN_Struct)->direction,
			s_ang,
			e_ang
            );
    }
	/** 楕円 **/
	else if(strcmp(IN_TypeName, "ELLIPSE") == 0 ){//7
		/* 出力フォーマット作成 */
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((Ellipse_Struct*)IN_Struct)->rotation_angle);
        m_FeatureData.EntityName = "ellipse_feature";
        m_FeatureData.EntityData.Format(
            "('%d','%d','%d','%d','%f','%f','%f','%f','%s')",
            ((Ellipse_Struct*)IN_Struct)->layer,
            ((Ellipse_Struct*)IN_Struct)->color,
            ((Ellipse_Struct*)IN_Struct)->type,
            ((Ellipse_Struct*)IN_Struct)->line_width,
            ((Ellipse_Struct*)IN_Struct)->center_x,
            ((Ellipse_Struct*)IN_Struct)->center_y,
            ((Ellipse_Struct*)IN_Struct)->radius_x,
            ((Ellipse_Struct*)IN_Struct)->radius_y,
			rot_ang
            );
    }
	/** 楕円弧 **/
	else if(strcmp(IN_TypeName , "ELLIPSE_ARC") == 0 ){//8
		/* 出力フォーマット作成 */
        /* 角度の出力桁を15桁にする */
		s_ang = SXFutyFunctionC::out_ctl(((Ellipse_Arc_Struct*)IN_Struct)->start_angle);
		e_ang = SXFutyFunctionC::out_ctl(((Ellipse_Arc_Struct*)IN_Struct)->end_angle);
		rot_ang = SXFutyFunctionC::out_ctl(((Ellipse_Arc_Struct*)IN_Struct)->rotation_angle);
        m_FeatureData.EntityName = "ellipse_arc_feature";
        m_FeatureData.EntityData.Format(
            "('%d','%d','%d','%d','%f','%f','%f','%f','%d','%s','%s','%s')",
            ((Ellipse_Arc_Struct*)IN_Struct)->layer,
            ((Ellipse_Arc_Struct*)IN_Struct)->color,
            ((Ellipse_Arc_Struct*)IN_Struct)->type,
            ((Ellipse_Arc_Struct*)IN_Struct)->line_width,
            ((Ellipse_Arc_Struct*)IN_Struct)->center_x,
            ((Ellipse_Arc_Struct*)IN_Struct)->center_y,
            ((Ellipse_Arc_Struct*)IN_Struct)->radius_x,
            ((Ellipse_Arc_Struct*)IN_Struct)->radius_y,
            ((Ellipse_Arc_Struct*)IN_Struct)->direction,
			rot_ang,
			s_ang,
			e_ang
            );
    }
	/** 文字要素 **/
	else if(strcmp(IN_TypeName, "TEXT") == 0 ){//9
		/* 出力フォーマット作成 */       
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((Text_Struct*)IN_Struct)->angle);
		slt_ang = SXFutyFunctionC::out_ctl(((Text_Struct*)IN_Struct)->slant);
        m_FeatureData.EntityName = "text_string_feature";
        m_FeatureData.EntityData.Format(
            "('%d','%d','%d',\\'%s\\','%f','%f','%f','%f','%f','%s','%s','%d','%d')",
            ((Text_Struct*)IN_Struct)->layer,
            ((Text_Struct*)IN_Struct)->color,
            ((Text_Struct*)IN_Struct)->font,
            ((Text_Struct*)IN_Struct)->str,
            ((Text_Struct*)IN_Struct)->text_x,
            ((Text_Struct*)IN_Struct)->text_y,
            ((Text_Struct*)IN_Struct)->height,
            ((Text_Struct*)IN_Struct)->width,
            ((Text_Struct*)IN_Struct)->spc,
			rot_ang,
			slt_ang,
            ((Text_Struct*)IN_Struct)->b_pnt,
            ((Text_Struct*)IN_Struct)->direct
            );
    }
	/** スプライン **/
	else if(strcmp(IN_TypeName, "SPLINE") == 0 ){//10
		/* 出力フォーマット作成 */
        m_FeatureData.EntityName = "spline_feature";
        CString xstring;
        CString tempstring;
        for(int i=0;i<( ((Spline_Struct*)IN_Struct)->number ); i++){
            if(i!=0) xstring += ",";
            tempstring.Format("%f",((Spline_Struct*)IN_Struct)->x[i]);
            xstring += tempstring;
        }
        CString ystring;
        for(i=0;i<( ((Spline_Struct*)IN_Struct)->number );i++){
            if(i!=0) ystring += ",";
            tempstring.Format("%f",((Spline_Struct*)IN_Struct)->y[i]);
            ystring += tempstring;
        }
        m_FeatureData.EntityData.Format(
            "('%d','%d','%d','%d','%d','%d','(%s)','(%s)')",
            ((Spline_Struct*)IN_Struct)->layer,
            ((Spline_Struct*)IN_Struct)->color,
            ((Spline_Struct*)IN_Struct)->type,
            ((Spline_Struct*)IN_Struct)->line_width,
            ((Spline_Struct*)IN_Struct)->open_close,
            ((Spline_Struct*)IN_Struct)->number,
            xstring,
            ystring
            );
    }
	/** クロソイド **/
	else if(strcmp(IN_TypeName, "CLOTHOID") == 0 ){//10
		/* 出力フォーマット作成 */
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((Clothoid_Struct*)IN_Struct)->angle);
        m_FeatureData.EntityName = "clothoid_feature";
        m_FeatureData.EntityData.Format(
            "('%d','%d','%d','%d','%f','%f','%f','%d','%s','%f','%f')",
            ((Clothoid_Struct*)IN_Struct)->layer,
            ((Clothoid_Struct*)IN_Struct)->color,
            ((Clothoid_Struct*)IN_Struct)->type,
            ((Clothoid_Struct*)IN_Struct)->line_width,
            ((Clothoid_Struct*)IN_Struct)->base_x,
            ((Clothoid_Struct*)IN_Struct)->base_y,
            ((Clothoid_Struct*)IN_Struct)->parameter,
            ((Clothoid_Struct*)IN_Struct)->direction,
            rot_ang,
            ((Clothoid_Struct*)IN_Struct)->start_length,
            ((Clothoid_Struct*)IN_Struct)->end_length
            );
    }
	/** 複合図形配置 **/
	else if(strcmp(IN_TypeName, "SFIG_LOCATE") == 0 ){//12
		/* 出力フォーマット作成 */
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((Sfigloc_Struct*)IN_Struct)->angle);
        m_FeatureData.EntityName = "sfig_locate_feature";
        m_FeatureData.EntityData.Format(
//			"('%d',\\'%s\\','%f','%f','%s','%f','%f')",
			"('%d',\\'%s\\','%f','%f','%s','%s','%s')",
            ((Sfigloc_Struct*)IN_Struct)->layer,
            ((Sfigloc_Struct*)IN_Struct)->name,
            ((Sfigloc_Struct*)IN_Struct)->x,
            ((Sfigloc_Struct*)IN_Struct)->y,
			rot_ang,
//			((Sfigloc_Struct*)IN_Struct)->ratio_x,
//			((Sfigloc_Struct*)IN_Struct)->ratio_y
			SXFutyFunctionC::out_ctl(((Sfigloc_Struct*)IN_Struct)->ratio_x),
			SXFutyFunctionC::out_ctl(((Sfigloc_Struct*)IN_Struct)->ratio_y)
            );
    }
	/** 既定義シンボル **/
	else if(strcmp(IN_TypeName, "EXTERNALLY_DEFINED_SYMBOL") == 0 ){//13
		/* 出力フォーマット作成 */        
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((Externally_Defined_Symbol_Struct*)IN_Struct)->rotate_angle);
        m_FeatureData.EntityName = "externally_defined_symbol_feature";
        m_FeatureData.EntityData.Format(
//			"('%d','%d','%d',\\'%s\\','%f','%f','%s','%f')",
			"('%d','%d','%d',\\'%s\\','%f','%f','%s','%s')",
            ((Externally_Defined_Symbol_Struct*)IN_Struct)->layer,
            ((Externally_Defined_Symbol_Struct*)IN_Struct)->color_flag,
            ((Externally_Defined_Symbol_Struct*)IN_Struct)->color,
            ((Externally_Defined_Symbol_Struct*)IN_Struct)->name,
            ((Externally_Defined_Symbol_Struct*)IN_Struct)->start_x,
            ((Externally_Defined_Symbol_Struct*)IN_Struct)->start_y,
			rot_ang,
//			((Externally_Defined_Symbol_Struct*)IN_Struct)->scale
			SXFutyFunctionC::out_ctl(((Externally_Defined_Symbol_Struct*)IN_Struct)->scale)
            );
    }
	/** 直線寸法 **/
	else if(strcmp(IN_TypeName , "LINEAR_DIMENSION") == 0 ){//14
		/* 出力フォーマット作成 */        
        CString section1;
        section1.Format(
            "'%d','%d','%d','%d','%f','%f','%f','%f'",
            ((LinearDim_Struct*)IN_Struct)->layer,
            ((LinearDim_Struct*)IN_Struct)->color,
            ((LinearDim_Struct*)IN_Struct)->type,
            ((LinearDim_Struct*)IN_Struct)->line_width,
            ((LinearDim_Struct*)IN_Struct)->sun_x1,
            ((LinearDim_Struct*)IN_Struct)->sun_y1,
            ((LinearDim_Struct*)IN_Struct)->sun_x2,
            ((LinearDim_Struct*)IN_Struct)->sun_y2
            );
        CString section2;
        section2.Format(
            "'%d','%f','%f','%f','%f','%f','%f','%d','%f','%f','%f','%f','%f','%f'",
            ((LinearDim_Struct*)IN_Struct)->flg2,
            ((LinearDim_Struct*)IN_Struct)->ho1_x0,
            ((LinearDim_Struct*)IN_Struct)->ho1_y0,
            ((LinearDim_Struct*)IN_Struct)->ho1_x1,
            ((LinearDim_Struct*)IN_Struct)->ho1_y1,
            ((LinearDim_Struct*)IN_Struct)->ho1_x2,
            ((LinearDim_Struct*)IN_Struct)->ho1_y2,
            ((LinearDim_Struct*)IN_Struct)->flg3,
            ((LinearDim_Struct*)IN_Struct)->ho2_x0,
            ((LinearDim_Struct*)IN_Struct)->ho2_y0,
            ((LinearDim_Struct*)IN_Struct)->ho2_x1,
            ((LinearDim_Struct*)IN_Struct)->ho2_y1,
            ((LinearDim_Struct*)IN_Struct)->ho2_x2,
            ((LinearDim_Struct*)IN_Struct)->ho2_y2
            );
        CString section3;
        section3.Format(
//			"'%d','%d','%f','%f','%f','%d','%d','%f','%f','%f'",
			"'%d','%d','%f','%f','%s','%d','%d','%f','%f','%s'",
            ((LinearDim_Struct*)IN_Struct)->arr1_code1,
            ((LinearDim_Struct*)IN_Struct)->arr1_code2,
            ((LinearDim_Struct*)IN_Struct)->arr1_x,
            ((LinearDim_Struct*)IN_Struct)->arr1_y,
//			((LinearDim_Struct*)IN_Struct)->arr1_r,
			SXFutyFunctionC::out_ctl(((LinearDim_Struct*)IN_Struct)->arr1_r),
            ((LinearDim_Struct*)IN_Struct)->arr2_code1,
            ((LinearDim_Struct*)IN_Struct)->arr2_code2,
            ((LinearDim_Struct*)IN_Struct)->arr2_x,
            ((LinearDim_Struct*)IN_Struct)->arr2_y,
//			((LinearDim_Struct*)IN_Struct)->arr2_r
			SXFutyFunctionC::out_ctl(((LinearDim_Struct*)IN_Struct)->arr2_r)
            );
        CString section4;
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((LinearDim_Struct*)IN_Struct)->angle);
		slt_ang = SXFutyFunctionC::out_ctl(((LinearDim_Struct*)IN_Struct)->slant);
        section4.Format(
            "'%d','%d',\\'%s\\','%f','%f','%f','%f','%f','%s','%s','%d','%d'",
            ((LinearDim_Struct*)IN_Struct)->flg4,
            ((LinearDim_Struct*)IN_Struct)->font,
            ((LinearDim_Struct*)IN_Struct)->str,
            ((LinearDim_Struct*)IN_Struct)->text_x,
            ((LinearDim_Struct*)IN_Struct)->text_y,
            ((LinearDim_Struct*)IN_Struct)->height,
            ((LinearDim_Struct*)IN_Struct)->width,
            ((LinearDim_Struct*)IN_Struct)->spc,
			rot_ang,
			slt_ang,
            ((LinearDim_Struct*)IN_Struct)->b_pnt,
            ((LinearDim_Struct*)IN_Struct)->direct
            );
        m_FeatureData.EntityName = "linear_dim_feature" ;
        m_FeatureData.EntityData.Format(
            "(%s,%s,%s,%s)",
            section1,
            section2,
            section3,
            section4
            );
    }
	/** 弧長寸法 **/
	else if(strcmp(IN_TypeName, "CURVE_DIMENSION") == 0 ){//15
		/* 出力フォーマット作成 */
        /* 角度の出力桁を15桁にする */
		s_ang = SXFutyFunctionC::out_ctl(((CurveDim_Struct*)IN_Struct)->sun_angle0);
		e_ang = SXFutyFunctionC::out_ctl(((CurveDim_Struct*)IN_Struct)->sun_angle1);
        CString section1;
        section1.Format(
            "'%d','%d','%d','%d','%f','%f','%f','%s','%s'",
            ((CurveDim_Struct*)IN_Struct)->layer,
            ((CurveDim_Struct*)IN_Struct)->color,
            ((CurveDim_Struct*)IN_Struct)->type,
            ((CurveDim_Struct*)IN_Struct)->line_width,
            ((CurveDim_Struct*)IN_Struct)->sun_x,
            ((CurveDim_Struct*)IN_Struct)->sun_y,
            ((CurveDim_Struct*)IN_Struct)->sun_radius,
			s_ang,
			e_ang
            );
        CString section2;
        section2.Format(
            "'%d','%f','%f','%f','%f','%f','%f','%d','%f','%f','%f','%f','%f','%f'",
            ((CurveDim_Struct*)IN_Struct)->flg2,
            ((CurveDim_Struct*)IN_Struct)->ho1_x0,
            ((CurveDim_Struct*)IN_Struct)->ho1_y0,
            ((CurveDim_Struct*)IN_Struct)->ho1_x1,
            ((CurveDim_Struct*)IN_Struct)->ho1_y1,
            ((CurveDim_Struct*)IN_Struct)->ho1_x2,
            ((CurveDim_Struct*)IN_Struct)->ho1_y2,
            ((CurveDim_Struct*)IN_Struct)->flg3,
            ((CurveDim_Struct*)IN_Struct)->ho2_x0,
            ((CurveDim_Struct*)IN_Struct)->ho2_y0,
            ((CurveDim_Struct*)IN_Struct)->ho2_x1,
            ((CurveDim_Struct*)IN_Struct)->ho2_y1,
            ((CurveDim_Struct*)IN_Struct)->ho2_x2,
            ((CurveDim_Struct*)IN_Struct)->ho2_y2
            );
        CString section3;
        section3.Format(
//			"'%d','%d','%f','%f','%f','%d','%d','%f','%f','%f'",
			"'%d','%d','%f','%f','%s','%d','%d','%f','%f','%s'",
            ((CurveDim_Struct*)IN_Struct)->arr1_code1,
            ((CurveDim_Struct*)IN_Struct)->arr1_code2,
            ((CurveDim_Struct*)IN_Struct)->arr1_x,
            ((CurveDim_Struct*)IN_Struct)->arr1_y,
//			((CurveDim_Struct*)IN_Struct)->arr1_r,
			SXFutyFunctionC::out_ctl(((CurveDim_Struct*)IN_Struct)->arr1_r),
            ((CurveDim_Struct*)IN_Struct)->arr2_code1,
            ((CurveDim_Struct*)IN_Struct)->arr2_code2,
            ((CurveDim_Struct*)IN_Struct)->arr2_x,
            ((CurveDim_Struct*)IN_Struct)->arr2_y,
//			((CurveDim_Struct*)IN_Struct)->arr2_r
			SXFutyFunctionC::out_ctl(((CurveDim_Struct*)IN_Struct)->arr2_r)
            );
        CString section4;
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((CurveDim_Struct*)IN_Struct)->angle);
		slt_ang = SXFutyFunctionC::out_ctl(((CurveDim_Struct*)IN_Struct)->slant);
        section4.Format(
            "'%d','%d',\\'%s\\','%f','%f','%f','%f','%f','%s','%s','%d','%d'",
            ((CurveDim_Struct*)IN_Struct)->flg4,
            ((CurveDim_Struct*)IN_Struct)->font,
            ((CurveDim_Struct*)IN_Struct)->str,
            ((CurveDim_Struct*)IN_Struct)->text_x,
            ((CurveDim_Struct*)IN_Struct)->text_y,
            ((CurveDim_Struct*)IN_Struct)->height,
            ((CurveDim_Struct*)IN_Struct)->width,
            ((CurveDim_Struct*)IN_Struct)->spc,
			rot_ang,
			slt_ang,
            ((CurveDim_Struct*)IN_Struct)->b_pnt,
            ((CurveDim_Struct*)IN_Struct)->direct
            );
        m_FeatureData.EntityName = "curve_dim_feature";
        m_FeatureData.EntityData.Format(
            "(%s,%s,%s,%s)",
            section1,
            section2,
            section3,
            section4
            );
    }
	/** 角度寸法 **/
	else if(strcmp(IN_TypeName, "ANGULAR_DIMENSION") == 0 ){//15
		/* 出力フォーマット作成 */
        /* 角度の出力桁を15桁にする */
		s_ang = SXFutyFunctionC::out_ctl(((AngularDim_Struct*)IN_Struct)->sun_angle0);
		e_ang = SXFutyFunctionC::out_ctl(((AngularDim_Struct*)IN_Struct)->sun_angle1);
        CString section1;
        section1.Format(
            "'%d','%d','%d','%d','%f','%f','%f','%s','%s'",
            ((AngularDim_Struct*)IN_Struct)->layer,
            ((AngularDim_Struct*)IN_Struct)->color,
            ((AngularDim_Struct*)IN_Struct)->type,
            ((AngularDim_Struct*)IN_Struct)->line_width,
            ((AngularDim_Struct*)IN_Struct)->sun_x,
            ((AngularDim_Struct*)IN_Struct)->sun_y,
            ((AngularDim_Struct*)IN_Struct)->sun_radius,
			s_ang,
			e_ang
            );
        CString section2;
        section2.Format(
            "'%d','%f','%f','%f','%f','%f','%f','%d','%f','%f','%f','%f','%f','%f'",
            ((AngularDim_Struct*)IN_Struct)->flg2,
            ((AngularDim_Struct*)IN_Struct)->ho1_x0,
            ((AngularDim_Struct*)IN_Struct)->ho1_y0,
            ((AngularDim_Struct*)IN_Struct)->ho1_x1,
            ((AngularDim_Struct*)IN_Struct)->ho1_y1,
            ((AngularDim_Struct*)IN_Struct)->ho1_x2,
            ((AngularDim_Struct*)IN_Struct)->ho1_y2,
            ((AngularDim_Struct*)IN_Struct)->flg3,
            ((AngularDim_Struct*)IN_Struct)->ho2_x0,
            ((AngularDim_Struct*)IN_Struct)->ho2_y0,
            ((AngularDim_Struct*)IN_Struct)->ho2_x1,
            ((AngularDim_Struct*)IN_Struct)->ho2_y1,
            ((AngularDim_Struct*)IN_Struct)->ho2_x2,
            ((AngularDim_Struct*)IN_Struct)->ho2_y2
            );
        CString section3;
        section3.Format(
//			"'%d','%d','%f','%f','%f','%d','%d','%f','%f','%f'",
			"'%d','%d','%f','%f','%s','%d','%d','%f','%f','%s'",
            ((AngularDim_Struct*)IN_Struct)->arr1_code1,
            ((AngularDim_Struct*)IN_Struct)->arr1_code2,
            ((AngularDim_Struct*)IN_Struct)->arr1_x,
            ((AngularDim_Struct*)IN_Struct)->arr1_y,
//			((AngularDim_Struct*)IN_Struct)->arr1_r,
			SXFutyFunctionC::out_ctl(((AngularDim_Struct*)IN_Struct)->arr1_r),
            ((AngularDim_Struct*)IN_Struct)->arr2_code1,
            ((AngularDim_Struct*)IN_Struct)->arr2_code2,
            ((AngularDim_Struct*)IN_Struct)->arr2_x,
            ((AngularDim_Struct*)IN_Struct)->arr2_y,
//			((AngularDim_Struct*)IN_Struct)->arr2_r
			SXFutyFunctionC::out_ctl(((AngularDim_Struct*)IN_Struct)->arr2_r)
            );
        CString section4;
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((AngularDim_Struct*)IN_Struct)->angle);
		slt_ang = SXFutyFunctionC::out_ctl(((AngularDim_Struct*)IN_Struct)->slant);
        section4.Format(
            "'%d','%d',\\'%s\\','%f','%f','%f','%f','%f','%s','%s','%d','%d'",
            ((AngularDim_Struct*)IN_Struct)->flg4,
            ((AngularDim_Struct*)IN_Struct)->font,
            ((AngularDim_Struct*)IN_Struct)->str,
            ((AngularDim_Struct*)IN_Struct)->text_x,
            ((AngularDim_Struct*)IN_Struct)->text_y,
            ((AngularDim_Struct*)IN_Struct)->height,
            ((AngularDim_Struct*)IN_Struct)->width,
            ((AngularDim_Struct*)IN_Struct)->spc,
			rot_ang,
			slt_ang,
            ((AngularDim_Struct*)IN_Struct)->b_pnt,
            ((AngularDim_Struct*)IN_Struct)->direct
            );
        m_FeatureData.EntityName = "angular_dim_feature";
        m_FeatureData.EntityData.Format(
            "(%s,%s,%s,%s)",
            section1,
            section2,
            section3,
            section4
            );
    }
	/** 半径寸法 **/
	else if(strcmp(IN_TypeName, "RADIUS_DIMENSION") == 0 ){//16
		/* 出力フォーマット作成 */
        CString section1;
        section1.Format(
//			"'%d','%d','%d','%d','%f','%f','%f','%f','%d','%d','%f','%f','%f'",
			"'%d','%d','%d','%d','%f','%f','%f','%f','%d','%d','%f','%f','%s'",
            ((RadiusDim_Struct*)IN_Struct)->layer,
            ((RadiusDim_Struct*)IN_Struct)->color,
            ((RadiusDim_Struct*)IN_Struct)->type,
            ((RadiusDim_Struct*)IN_Struct)->line_width,
            ((RadiusDim_Struct*)IN_Struct)->sun_x1,
            ((RadiusDim_Struct*)IN_Struct)->sun_y1,
            ((RadiusDim_Struct*)IN_Struct)->sun_x2,
            ((RadiusDim_Struct*)IN_Struct)->sun_y2,
            ((RadiusDim_Struct*)IN_Struct)->arr_code1,
            ((RadiusDim_Struct*)IN_Struct)->arr_code2,
            ((RadiusDim_Struct*)IN_Struct)->arr_x,
            ((RadiusDim_Struct*)IN_Struct)->arr_y,
//			((RadiusDim_Struct*)IN_Struct)->arr_r
			SXFutyFunctionC::out_ctl(((RadiusDim_Struct*)IN_Struct)->arr_r)
            );
        CString section2;
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((RadiusDim_Struct*)IN_Struct)->angle);
		slt_ang = SXFutyFunctionC::out_ctl(((RadiusDim_Struct*)IN_Struct)->slant);
        section2.Format(
            "'%d','%d',\\'%s\\','%f','%f','%f','%f','%f','%s','%s','%d','%d'",
            ((RadiusDim_Struct*)IN_Struct)->flg,
            ((RadiusDim_Struct*)IN_Struct)->font,
            ((RadiusDim_Struct*)IN_Struct)->str,
            ((RadiusDim_Struct*)IN_Struct)->text_x,
            ((RadiusDim_Struct*)IN_Struct)->text_y,
            ((RadiusDim_Struct*)IN_Struct)->height,
            ((RadiusDim_Struct*)IN_Struct)->width,
            ((RadiusDim_Struct*)IN_Struct)->spc,
			rot_ang,
			slt_ang,
            ((RadiusDim_Struct*)IN_Struct)->b_pnt,
            ((RadiusDim_Struct*)IN_Struct)->direct
            );
        m_FeatureData.EntityName = "radius_dim_feature";
        m_FeatureData.EntityData.Format(
            "(%s,%s)",
            section1,
            section2
            );
    }
	/** 直径寸法 **/
	else if(strcmp(IN_TypeName, "DIAMETER_DIMENSION") == 0 ){//17
		/* 出力フォーマット作成 */
        CString section1;
        section1.Format(
            "'%d','%d','%d','%d','%f','%f','%f','%f'",
            ((DiameterDim_Struct*)IN_Struct)->layer,
            ((DiameterDim_Struct*)IN_Struct)->color,
            ((DiameterDim_Struct*)IN_Struct)->type,
            ((DiameterDim_Struct*)IN_Struct)->line_width,
            ((DiameterDim_Struct*)IN_Struct)->sun_x1,
            ((DiameterDim_Struct*)IN_Struct)->sun_y1,
            ((DiameterDim_Struct*)IN_Struct)->sun_x2,
            ((DiameterDim_Struct*)IN_Struct)->sun_y2
            );
        CString section2;
        section2.Format(
//			"'%d','%d','%f','%f','%f','%d','%d','%f','%f','%f'",
			"'%d','%d','%f','%f','%s','%d','%d','%f','%f','%s'",
            ((DiameterDim_Struct*)IN_Struct)->arr1_code1,
            ((DiameterDim_Struct*)IN_Struct)->arr1_code2,
            ((DiameterDim_Struct*)IN_Struct)->arr1_x,
            ((DiameterDim_Struct*)IN_Struct)->arr1_y,
//			((DiameterDim_Struct*)IN_Struct)->arr1_r,
			SXFutyFunctionC::out_ctl(((DiameterDim_Struct*)IN_Struct)->arr1_r),
            ((DiameterDim_Struct*)IN_Struct)->arr2_code1,
            ((DiameterDim_Struct*)IN_Struct)->arr2_code2,
            ((DiameterDim_Struct*)IN_Struct)->arr2_x,
            ((DiameterDim_Struct*)IN_Struct)->arr2_y,
//			((DiameterDim_Struct*)IN_Struct)->arr2_r
			SXFutyFunctionC::out_ctl(((DiameterDim_Struct*)IN_Struct)->arr2_r)
            );
        CString section3;
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((DiameterDim_Struct*)IN_Struct)->angle);
		slt_ang = SXFutyFunctionC::out_ctl(((DiameterDim_Struct*)IN_Struct)->slant);
        section3.Format(
            "'%d','%d',\\'%s\\','%f','%f','%f','%f','%f','%s','%s','%d','%d'",
            ((DiameterDim_Struct*)IN_Struct)->flg,
            ((DiameterDim_Struct*)IN_Struct)->font,
            ((DiameterDim_Struct*)IN_Struct)->str,
            ((DiameterDim_Struct*)IN_Struct)->text_x,
            ((DiameterDim_Struct*)IN_Struct)->text_y,
            ((DiameterDim_Struct*)IN_Struct)->height,
            ((DiameterDim_Struct*)IN_Struct)->width,
            ((DiameterDim_Struct*)IN_Struct)->spc,
			rot_ang,
			slt_ang,
            ((DiameterDim_Struct*)IN_Struct)->b_pnt,
            ((DiameterDim_Struct*)IN_Struct)->direct
            );
        m_FeatureData.EntityName = "diameter_dim_feature";
        m_FeatureData.EntityData.Format(
            "(%s,%s,%s)",
            section1,
            section2,
            section3
            );
    }
	/** 引出し線 **/
	else if(strcmp(IN_TypeName , "LABEL") == 0 ){//18
		/* 出力フォーマット作成 */
        CString section1;
        section1.Format(
            "'%d','%d','%d','%d','%d'",
            ((Label_Struct*)IN_Struct)->layer,
            ((Label_Struct*)IN_Struct)->color,
            ((Label_Struct*)IN_Struct)->type,
            ((Label_Struct*)IN_Struct)->line_width,
            ((Label_Struct*)IN_Struct)->vertex_number
            );
        CString xstring;
        CString tempstring;
        for(int i=0;i<( ((Label_Struct*)IN_Struct)->vertex_number ); i++){
            if(i!=0) xstring += ",";
            tempstring.Format("%f",((Label_Struct*)IN_Struct)->vertex_x[i]);
            xstring += tempstring;
        }
        CString ystring;
        for(i=0;i<( ((Label_Struct*)IN_Struct)->vertex_number ); i++){
            if(i!=0) ystring += ",";
            tempstring.Format("%f",((Label_Struct*)IN_Struct)->vertex_y[i]);
            ystring += tempstring;
        }
        CString section2;
        section2.Format(
//			"'%d','%f'",
			"'%d','%s'",
            ((Label_Struct*)IN_Struct)->arr_code,
//			((Label_Struct*)IN_Struct)->arr_r
			SXFutyFunctionC::out_ctl(((Label_Struct*)IN_Struct)->arr_r)
            );
        CString section3;
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((Label_Struct*)IN_Struct)->angle);
		slt_ang = SXFutyFunctionC::out_ctl(((Label_Struct*)IN_Struct)->slant);
        section3.Format(
            "'%d','%d',\\'%s\\','%f','%f','%f','%f','%f','%s','%s','%d','%d'",
            ((Label_Struct*)IN_Struct)->flg,
            ((Label_Struct*)IN_Struct)->font,
            ((Label_Struct*)IN_Struct)->str,
            ((Label_Struct*)IN_Struct)->text_x,
            ((Label_Struct*)IN_Struct)->text_y,
            ((Label_Struct*)IN_Struct)->height,
            ((Label_Struct*)IN_Struct)->width,
            ((Label_Struct*)IN_Struct)->spc,
			rot_ang,
			slt_ang,
            ((Label_Struct*)IN_Struct)->b_pnt,
            ((Label_Struct*)IN_Struct)->direct
            );
        m_FeatureData.EntityName = "label_feature";
        m_FeatureData.EntityData.Format(
            "(%s,'(%s)','(%s)',%s,%s)",
            section1,
            xstring,
            ystring,
            section2,
            section3
            );
    }
	/** バルーン **/
	else if(strcmp(IN_TypeName, "BALLOON") == 0 ){//19
		/* 出力フォーマット作成 */
        CString section1;
        section1.Format(
            "'%d','%d','%d','%d','%d'",
            ((Balloon_Struct*)IN_Struct)->layer,
            ((Balloon_Struct*)IN_Struct)->color,
            ((Balloon_Struct*)IN_Struct)->type,
            ((Balloon_Struct*)IN_Struct)->line_width,
            ((Balloon_Struct*)IN_Struct)->vertex_number
            );
        CString xstring;
        CString tempstring;
        for(int i=0;i<( ((Balloon_Struct*)IN_Struct)->vertex_number ); i++){
            if(i!=0) xstring += ",";
            tempstring.Format("%f",((Balloon_Struct*)IN_Struct)->vertex_x[i]);
            xstring += tempstring;
        }
        CString ystring;
        for(i=0;i<( ((Balloon_Struct*)IN_Struct)->vertex_number ); i++){
            if(i!=0) ystring += ",";
            tempstring.Format("%f",((Balloon_Struct*)IN_Struct)->vertex_y[i]);
            ystring += tempstring;
        }
        CString section2;
        section2.Format(
//			"'%f','%f','%f','%d','%f'",
			"'%f','%f','%f','%d','%s'",
            ((Balloon_Struct*)IN_Struct)->center_x,
            ((Balloon_Struct*)IN_Struct)->center_y,
            ((Balloon_Struct*)IN_Struct)->radius,
            ((Balloon_Struct*)IN_Struct)->arr_code,
//			((Balloon_Struct*)IN_Struct)->arr_r
			SXFutyFunctionC::out_ctl(((Balloon_Struct*)IN_Struct)->arr_r)
            );
        CString section3;
        /* 角度の出力桁を15桁にする */
		rot_ang = SXFutyFunctionC::out_ctl(((Balloon_Struct*)IN_Struct)->angle);
		slt_ang = SXFutyFunctionC::out_ctl(((Balloon_Struct*)IN_Struct)->slant);
        section3.Format(
            "'%d','%d',\\'%s\\','%f','%f','%f','%f','%f','%s','%s','%d','%d'",
            ((Balloon_Struct*)IN_Struct)->flg,
            ((Balloon_Struct*)IN_Struct)->font,
            ((Balloon_Struct*)IN_Struct)->str,
            ((Balloon_Struct*)IN_Struct)->text_x,
            ((Balloon_Struct*)IN_Struct)->text_y,
            ((Balloon_Struct*)IN_Struct)->height,
            ((Balloon_Struct*)IN_Struct)->width,
            ((Balloon_Struct*)IN_Struct)->spc,
			rot_ang,
			slt_ang,
            ((Balloon_Struct*)IN_Struct)->b_pnt,
            ((Balloon_Struct*)IN_Struct)->direct
            );
        m_FeatureData.EntityName = "balloon_feature";
        m_FeatureData.EntityData.Format(
            "(%s,'(%s)','(%s)',%s,%s)",
            section1,
            xstring,
            ystring,
            section2,
            section3
            );
    }
	/** 外部定義ハッチング **/
	else if(strcmp(IN_TypeName , "EXTERNALLY_DEFINED_HATCH") == 0 ){//20
		/* 出力フォーマット作成 */
        CString inidstring;
        CString tempstring;
        for(int i=0;i<( ((Externally_Defined_Hatch_Struct*)IN_Struct)->number ); i++){
            if(i!=0) inidstring += ",";
            tempstring.Format("%d",((Externally_Defined_Hatch_Struct*)IN_Struct)->in_id[i]);
            inidstring += tempstring;
        }
        m_FeatureData.EntityName = "externally_defined_hatch_feature";
        m_FeatureData.EntityData.Format(
            "('%d',\\'%s\\','%d','%d','(%s)')",
            ((Externally_Defined_Hatch_Struct*)IN_Struct)->layer,
            ((Externally_Defined_Hatch_Struct*)IN_Struct)->name,
            ((Externally_Defined_Hatch_Struct*)IN_Struct)->out_id,
            ((Externally_Defined_Hatch_Struct*)IN_Struct)->number,
            inidstring
            );
    }
	/** ハッチング(塗り) **/
	else if(strcmp(IN_TypeName, "FILL_AREA_STYLE_COLOUR") == 0 ){//21
		/* 出力フォーマット作成 */
        CString inidstring;
        CString tempstring;
        for(int i=0;i<( ((Fill_area_style_colour_Struct*)IN_Struct)->number ); i++){
            if(i!=0) inidstring += ",";
            tempstring.Format("%d",((Fill_area_style_colour_Struct*)IN_Struct)->in_id[i]);
            inidstring += tempstring;
        }
        m_FeatureData.EntityName = "fill_area_style_colour_feature";
        m_FeatureData.EntityData.Format(
            "('%d','%d','%d','%d','(%s)')",
            ((Fill_area_style_colour_Struct*)IN_Struct)->layer,
            ((Fill_area_style_colour_Struct*)IN_Struct)->color,
            ((Fill_area_style_colour_Struct*)IN_Struct)->out_id,
            ((Fill_area_style_colour_Struct*)IN_Struct)->number,
            inidstring
            );
    }
	/** ハッチング(ユーザ定義) **/
	else if(strcmp(IN_TypeName , "FILL_AREA_STYLE_HATCHING") == 0 ){//22
		/* 出力フォーマット作成 */
        CString tempstring;
        CString datastring;
        for(int i=0;i<( ((Fill_area_style_hatching_Struct*)IN_Struct)->hatch_number ); i++){
			/* 角度の出力桁を15桁にする */
			rot_ang = SXFutyFunctionC::out_ctl(((Fill_area_style_hatching_Struct*)IN_Struct)->hatch_angle[i]);
            if(i!=0) datastring += ",";
            tempstring.Format("'(%d,%d,%d,%f,%f,%f,%s)'",
                ((Fill_area_style_hatching_Struct*)IN_Struct)->hatch_color[i],
                ((Fill_area_style_hatching_Struct*)IN_Struct)->hatch_type[i],
                ((Fill_area_style_hatching_Struct*)IN_Struct)->hatch_line_width[i],
                ((Fill_area_style_hatching_Struct*)IN_Struct)->hatch_start_x[i],
                ((Fill_area_style_hatching_Struct*)IN_Struct)->hatch_start_y[i],
                ((Fill_area_style_hatching_Struct*)IN_Struct)->hatch_spacing[i],
				rot_ang
                );
            datastring += tempstring;
        }
        CString inidstring;
        for(i=0;i<( ((Fill_area_style_hatching_Struct*)IN_Struct)->number ); i++){
            if(i!=0) inidstring += ",";
            tempstring.Format("%d",((Fill_area_style_hatching_Struct*)IN_Struct)->in_id[i]);
            inidstring += tempstring;
        }
        m_FeatureData.EntityName = "fill_area_style_hatching_feature";
        m_FeatureData.EntityData.Format(
            "('%d','%d',%s,'%d','%d','(%s)')",
            ((Fill_area_style_hatching_Struct*)IN_Struct)->layer,
            ((Fill_area_style_hatching_Struct*)IN_Struct)->hatch_number,
            datastring,
            ((Fill_area_style_hatching_Struct*)IN_Struct)->out_id,
            ((Fill_area_style_hatching_Struct*)IN_Struct)->number,
            inidstring
            );
    }
	/** ハッチング(パターン) **/
	else if(strcmp(IN_TypeName, "FILL_AREA_STYLE_TILES") == 0 ){//23
		/* 出力フォーマット作成 */
        CString inidstring;
        CString tempstring;
        for(int i=0;i<( ((Fill_area_style_tiles_Struct*)IN_Struct)->number ); i++){
            if(i!=0) inidstring += ",";
            tempstring.Format("%d",((Fill_area_style_tiles_Struct*)IN_Struct)->in_id[i]);
            inidstring += tempstring;
        }
		/* 角度の出力桁を15桁にする */
		s_ang = SXFutyFunctionC::out_ctl(((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_vector1_angle);
		e_ang = SXFutyFunctionC::out_ctl(((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_vector2_angle);
		rot_ang = SXFutyFunctionC::out_ctl(((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_angle);
        m_FeatureData.EntityName = "fill_area_style_tiles_feature";
        m_FeatureData.EntityData.Format(
//			"('%d',\\'%s\\','%d','%f','%f','%f','%s','%f','%s','%f','%f','%s','%d','%d','(%s)')",
			"('%d',\\'%s\\','%d','%f','%f','%f','%s','%f','%s','%s','%s','%s','%d','%d','(%s)')",
            ((Fill_area_style_tiles_Struct*)IN_Struct)->layer,
            ((Fill_area_style_tiles_Struct*)IN_Struct)->name,
            ((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_color,
            ((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_x,
            ((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_y,
            ((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_vector1,
			s_ang,
            ((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_vector2,
			e_ang,
//			((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_scale_x,
//			((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_scale_y,
			SXFutyFunctionC::out_ctl(((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_scale_x),
			SXFutyFunctionC::out_ctl(((Fill_area_style_tiles_Struct*)IN_Struct)->hatch_pattern_scale_y),
			rot_ang,
            ((Fill_area_style_tiles_Struct*)IN_Struct)->out_id,
            ((Fill_area_style_tiles_Struct*)IN_Struct)->number,
            inidstring
            );
    }

//	ADD(S)	Ver3.0対応	K.Naono	02.12.03
	/** 図面表題欄 **/
	else if(strcmp(IN_TypeName, "ATTRIBUTE") == 0 ){//24
		m_FeatureData.EntityName = "drawing_attribute_feature";
		m_FeatureData.EntityData.Format(
			"(\\'%s\\',\\'%s\\',\\'%s\\',\\'%s\\',\\'%s\\',\\'%s\\',\\'%s\\','%d','%d','%d',\\'%s\\',\\'%s\\')",
			((Attribute_Struct*)IN_Struct)->p_name,
			((Attribute_Struct*)IN_Struct)->c_name,
			((Attribute_Struct*)IN_Struct)->c_type,
			((Attribute_Struct*)IN_Struct)->d_title,
			((Attribute_Struct*)IN_Struct)->d_number,
			((Attribute_Struct*)IN_Struct)->d_type,
			((Attribute_Struct*)IN_Struct)->d_scale,
			((Attribute_Struct*)IN_Struct)->d_year,
			((Attribute_Struct*)IN_Struct)->d_month,
			((Attribute_Struct*)IN_Struct)->d_day,
			((Attribute_Struct*)IN_Struct)->c_contractor,
			((Attribute_Struct*)IN_Struct)->c_owner
			);
	}
//	ADD(E)	Ver3.0対応	K.Naono	02.12.03
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.04.17
//
//  １．機能概要
//      Instance Data 構造体の取得(WRITE)
//
//  ２．インターフェース
//      INSTANCEDATA* GetFeatureData( )
//	    引数: 型			  引数名			    内容
//
//	    復帰値	
//            INSTANCEDATA*		Instance Data構造体のアドレス
//      
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.17     周
//
//------------------------------------------------------------
INSTANCEDATA* SXFFeatureC::GetFeatureData( )
{
    return &m_FeatureData;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.04.17
//
//  １．機能概要
//      構造体アドレスの取得(READ)
//
//  ２．インターフェース
//      LPVOID GetStructData(char*& OUT_Type,size_t TypeSize)
//
//		引数: 型			引数名			内容
//				char*&		OUT_Type		構造体のTypeのアドレス
//				size_t		NameSize,		フィーチャ名バッファサイズ
//	    復帰値	
//            正常時：構造体のアドレス
//			  異常時：NULL
//
//  ３．履歴
//		履歴番号	日付け			担当者			 概要
//		作成		2000.04.17		周
//		更新		2015.04.27		T.Matsushima	JPCERT/CC 指摘 脆弱性対応
//
//------------------------------------------------------------
//LPVOID SXFFeatureC::GetStructData(char*& OUT_Type)
LPVOID SXFFeatureC::GetStructData(char*& OUT_Type,size_t TypeSize)
{
//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
//	strcpy(OUT_Type,m_TypeName);
	strcpy_s(OUT_Type,TypeSize,m_TypeName);
	return m_StructData;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.04.17
//
//  １．機能概要
//      Assembly構造体アドレスの取得(READ)
//      
//  ２．インターフェース
//      LPVOID GetAssemblyStructData()
//
//	    引数: 型			  引数名			    内容
//			  なし
//
//	    復帰値	
//            正常時：Assembly構造体のアドレス
//			  異常時：NULL
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.17     周
//
//------------------------------------------------------------
LPVOID SXFFeatureC::GetAssemblyStructData()
{
    return m_StructData;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.04.17
//
//  １．機能概要
//      Assembly構造体からFeature Comment data に変換する(WRITE)
//      
//  ２．インターフェース
//      void SetAssemblyData(int IN_Type, LPVOID IN_Struct)
//
//	    引数: 型		引数名		内容
//            int       IN_Type     Assembly構造体のType
//            LPVOID    IN_Struct   Assembly構造体のアドレス
//
//	    復帰値	
//			  なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.17     周
//
//------------------------------------------------------------
void SXFFeatureC::SetAssemblyData(int IN_Type, LPVOID IN_Struct)
{
	int x = 0;
	int y = 0;

	/* インスタンスモード */
	m_FeatureData.InstanceMode = SXF_IM_FT_ASSEMBLY;        
	/* インスタンスID */
	m_FeatureData.InstanceID = m_InstanceIDManager->GetNewInstanceID();
	switch(IN_Type){
	case 1:
		/** 用紙 **/
		//用紙サイズの設定
		switch(((Sheet_Struct*)IN_Struct)->type){
			case 9:			/* FREE */
				x = ((Sheet_Struct*)IN_Struct)->x;
				y = ((Sheet_Struct*)IN_Struct)->y;
				break;
			default:
				x = GetSheetX(((Sheet_Struct*)IN_Struct)->type);
				y = GetSheetY(((Sheet_Struct*)IN_Struct)->type);
				break;
		}
		/* 出力フォーマット作成 */
        m_FeatureData.EntityName = "drawing_sheet_feature";
		if (((Sheet_Struct*)IN_Struct)->orient == 1		/* 横 */
			|| ((Sheet_Struct*)IN_Struct)->type == 9){	/* FREE */
			m_FeatureData.EntityData.Format("(\\'%s\\','%d','%d','%d','%d')",
				((Sheet_Struct*)IN_Struct)->name,
				((Sheet_Struct*)IN_Struct)->type,
				((Sheet_Struct*)IN_Struct)->orient,
				x,
				y
				);
		}
		else
			m_FeatureData.EntityData.Format("(\\'%s\\','%d','%d','%d','%d')",
				((Sheet_Struct*)IN_Struct)->name,
				((Sheet_Struct*)IN_Struct)->type,
				((Sheet_Struct*)IN_Struct)->orient,
				y,
				x
				);
		
		break;
	case 2:
		/** 複合図形定義 **/
		/* 出力フォーマット作成 */
        m_FeatureData.EntityName = "sfig_org_feature";
        m_FeatureData.EntityData.Format(
            "(\\'%s\\','%d')",
            ((Sfigorg_Struct*)IN_Struct)->name,
            ((Sfigorg_Struct*)IN_Struct)->flag
            );
		break;
	case 3:
		/** 複合曲線定義 **/
		/* 出力フォーマット作成 */
        m_FeatureData.EntityName = "composite_curve_org_feature";
        m_FeatureData.EntityData.Format(
            "('%d','%d','%d','%d')",
            ((Ccurve_Org_Struct*)IN_Struct)->color,
            ((Ccurve_Org_Struct*)IN_Struct)->type,
            ((Ccurve_Org_Struct*)IN_Struct)->line_width,
            ((Ccurve_Org_Struct*)IN_Struct)->flag
            );
		break;
    }
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2001.03.21
//
//  １．機能概要
//      フィーチャ構造体返却済みフラグをONにする(READ)
//      
//  ２．インターフェース
//      void SetReturnFlag()
//
//	    引数: 型		引数名		内容
//			  なし
//
//	    復帰値	
//			  なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2001.03.21     宮園
//
//------------------------------------------------------------
void SXFFeatureC::SetReturnFlag()
{
	m_ReturnFlag = 1;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.08.29
//
//  １．機能概要
//      用紙サイズ種別にあった、X方向長さを得る
//      
//  ２．インターフェース
//      int GetSheetX(int IN_Type)
//
//	    引数: 型		引数名		内容
//            int       IN_Type     用紙サイズ種別
//							=0:A0
//							=1:A1
//							=2:A2
//							=3:A3
//							=4:A4
//	    復帰値	
//			正常時	：int X方向サイズ
//			異常時	：-1(用紙サイズ種別不正）
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.08.29     
//
//------------------------------------------------------------
int SXFFeatureC::GetSheetX(int IN_Type)
{
	int x=0;
	switch(IN_Type){
		/* A0 */
		case 0:
			x = 1189;
			break;
		/* A1 */
		case 1:
			x = 841;
			break;
		/* A2 */
		case 2:
			x = 594;
			break;
		/* A3 */
		case 3:
			x = 420;
			break;
		/* A4 */
		case 4:
			x = 297;
			break;
		/* ERR */
		default:
			return -1;
			
	}
	return x;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFFeatureC.cpp                        作成日：2000.08.29
//
//  １．機能概要
//      用紙サイズ種別にあった、Y方向長さを得る
//      
//  ２．インターフェース
//      int GetSheetX(int IN_Type)
//
//	    引数: 型		引数名		内容
//            int       IN_Type     用紙サイズ種別
//							=0:A0
//							=1:A1
//							=2:A2
//							=3:A3
//							=4:A4
//	    復帰値	
//			正常時	：int Y方向サイズ
//			異常時	：-1(用紙サイズ種別不正）
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.08.29     
//
//------------------------------------------------------------
int SXFFeatureC::GetSheetY(int IN_Type)
{
	int y=0;
	switch(IN_Type){
		/* A0 */
		case 0:
			y = 841;
			break;
		/* A1 */
		case 1:
			y = 594;
			break;
		/* A2 */
		case 2:
			y = 420;
			break;
		/* A3 */
		case 3:
			y = 297;
			break;
		/* A4 */
		case 4:
			y = 210;
			break;
		/* ERR */
		default:
			return -1;
	}
	return y;
}

