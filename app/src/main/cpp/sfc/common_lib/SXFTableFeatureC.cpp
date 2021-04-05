//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableFeatureC.cpp                    作成日：2000.04.10
//
//	１．クラス概要
//      定義テーブルフィーチャーデータを保持する
//
//	２．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------

#include "stdafx.h"
#include "SXFDataStructC.h"
#include "SXFTableFeatureC.h"
#include "SXFStruct.h"
#include "SXFLayerC.h"
#include "SXFPreDefFontC.h"
#include "SXFUserDefFontC.h"
#include "SXFPreDefColorC.h"
#include "SXFUserDefColorC.h"
#include "SXFWidthC.h"
#include "SXFTextFontC.h"
#include "SXFInstanceIDC.h"
#include "SXFLayerC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableFeatureC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      クラス構築
//
//  ２．インターフェース
//		void	SXFTableFeatureC::SXFTableFeatureC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
SXFTableFeatureC::SXFTableFeatureC()
{
    m_FeatureData.EntityData.Empty();
    m_FeatureData.EntityName.Empty();
    m_FeatureData.InstanceMode = -1;
    m_FeatureData.InstanceID = -1;
    m_StructData=NULL;
	m_InstanceIDManager = NULL;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableFeatureC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      クラス構築
//
//  ２．インターフェース
//		void	SXFTableFeatureC::SXFTableFeatureC(SXFInstanceIDC* IN_InstanceIDManager)
//		
//	引数	：	型				引数名					内容
//				SXFInstanceIDC* IN_InstanceIDManager	ｲﾝｽﾀﾝｽIDｵﾌﾞｼﾞｪｸﾄのｱﾄﾞﾚｽ
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
SXFTableFeatureC::SXFTableFeatureC(SXFInstanceIDC* IN_InstanceIDManager)
{
    m_FeatureData.EntityData.Empty();
    m_FeatureData.EntityName.Empty();
    m_FeatureData.InstanceMode = -1;
    m_FeatureData.InstanceID = -1;
    m_StructData=NULL;
	m_InstanceIDManager = IN_InstanceIDManager;
}
//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableFeatureC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      クラス消滅
//
//  ２．インターフェース
//		void	SXFTableFeatureC::~SXFTableFeatureC()
//		
//	引数	：	型		引数名				内容
//		なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------

SXFTableFeatureC::~SXFTableFeatureC()
{
    //delete struce from memory
    if(m_StructData!=NULL)
        delete m_StructData;   
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableFeatureC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      Table Feature Class Dataを設定して、構造体生成(READ)
// 
//  ２．インターフェース
//      void SetStructData(int IN_Type, LPVOID IN_Class)
//
//	    引数: 型			  引数名			    内容
//            int            IN_Type,              Class Type
//            LPVOID         IN_Class              Table Classの
//                                                 アドレス 
//      
//	    復帰値:	
//            なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
void SXFTableFeatureC::SetStructData(int IN_Type, LPVOID IN_Class)
{
	int i= 0;
	Layer_Struct* layer;
	Predefined_Colour_Struct* predefcolor;
	Userdefined_Colour_Struct* userdefcolor;
	Predefined_Linetype_Struct* predeffont;
	Userdefined_Linetype_Struct* userdeffont;
	Line_Width_Struct* width;
	Text_Font_Struct* textfont;

    switch(IN_Type){
		case 1://layer
			/* ﾚｲﾔ構造体生成 */
			layer = new Layer_Struct;
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(layer->name,((SXFLayerC*)IN_Class)->m_Name);
			strcpy_s(layer->name,_countof(layer->name),((SXFLayerC*)IN_Class)->m_Name);
			layer->lflag = ((SXFLayerC*)IN_Class)->m_LfFlag;
			m_StructData = layer;
			break;
		case 2://predefcolor
			/* 既定義色構造体生成 */
			predefcolor= new Predefined_Colour_Struct;
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(predefcolor->name, ((SXFPreDefColorC*)IN_Class)->m_Name);
			strcpy_s(predefcolor->name,_countof(predefcolor->name), ((SXFPreDefColorC*)IN_Class)->m_Name);
			m_StructData = predefcolor;
			break;
		case 3://userdefcolor
			/* ﾕｰｻﾞ定義色構造体生成 */
			userdefcolor =new Userdefined_Colour_Struct;
			userdefcolor->red=((SXFUserDefColorC*)IN_Class)->R;
			userdefcolor->green=((SXFUserDefColorC*)IN_Class)->G;
			userdefcolor->blue=((SXFUserDefColorC*)IN_Class)->B;
			m_StructData = userdefcolor;        
			break;
		case 4://predeffont
			/* 既定義線種構造体生成 */
			predeffont = new Predefined_Linetype_Struct;
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(predeffont->name,((SXFPreDefFontC*)IN_Class)->m_Name);
			strcpy_s(predeffont->name,_countof(predeffont->name),((SXFPreDefFontC*)IN_Class)->m_Name);
			m_StructData = predeffont;
			break;
		case 5://userdeffont
			/* ﾕｰｻﾞ定義線種構造体生成 */
			userdeffont = new Userdefined_Linetype_Struct;
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(userdeffont->name,((SXFUserDefFontC*)IN_Class)->m_Name);
			strcpy_s(userdeffont->name,_countof(userdeffont->name),((SXFUserDefFontC*)IN_Class)->m_Name);
			userdeffont->segment=((SXFUserDefFontC*)IN_Class)->m_Segment;
			for(i=0;i<userdeffont->segment;i++){
				userdeffont->pitch[i]=((SXFUserDefFontC*)IN_Class)->m_Pitch[i];
			}
			m_StructData = userdeffont;
			break;
		case 6://width
			/* 線幅構造体生成 */
			width = new Line_Width_Struct;
			width->width=((SXFWidthC*)IN_Class)->m_Width;
			m_StructData = width;
			break;
		case 7://textfont
			/* 文字ﾌｫﾝﾄ構造体生成 */
			textfont = new Text_Font_Struct;
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(textfont->name,((SXFTextFontC*)IN_Class)->m_Name);
			strcpy_s(textfont->name,_countof(textfont->name),((SXFTextFontC*)IN_Class)->m_Name);
			m_StructData =  textfont;
			break;
    }
}


//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableFeatureC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      Table 構造体を設定して、Feature Data生成(WRITE)
// 
//  ２．インターフェース
//      void SetFeatureData(int IN_Type, LPVOID IN_Struct)
//
//	    引数: 型			  引数名			    内容
//            int            IN_Type,              構造体のType
//            LPVOID         IN_Struct             構造体のアドレス
//                                                  
//      
//	    復帰値:	
//            なし
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
void SXFTableFeatureC::SetFeatureData(int IN_Type, LPVOID IN_Struct)
{
    int i =0;
	CString pitchstring;
    CString tempstring;

	/* インスタンスモード */
	m_FeatureData.InstanceMode=SXF_IM_FT_TABLE;
	//can not to set Instance ID at here ,
	//please get ID from ID Manager class & Set it
	//before write this feature to a .stp file
	m_FeatureData.InstanceID = m_InstanceIDManager->GetNewInstanceID();
    switch(IN_Type){
		case 2://predefcolor
			m_FeatureData.EntityName = "pre_defined_colour_feature";
			m_FeatureData.EntityData.Format("(\\'%s\\')",
				((Predefined_Colour_Struct*)IN_Struct)->name);
			break;
		case 3://userdefcolor
			m_FeatureData.EntityName = "user_defined_colour_feature";
			m_FeatureData.EntityData.Format("('%d','%d','%d')",
				((Userdefined_Colour_Struct*)IN_Struct)->red,
				((Userdefined_Colour_Struct*)IN_Struct)->green,
				((Userdefined_Colour_Struct*)IN_Struct)->blue
				);
			break;
		case 4://predeffont
			m_FeatureData.EntityName = "pre_defined_font_feature";
			m_FeatureData.EntityData.Format("(\\'%s\\')",
				((Predefined_Linetype_Struct*)IN_Struct)->name);
			break;
		case 5://userdeffont
			m_FeatureData.EntityName = "user_defined_font_feature";
			for(i=0;i<((Userdefined_Linetype_Struct*)IN_Struct)->segment;i++){
				if(i!=0) pitchstring+=",";
				tempstring.Format("%f",((Userdefined_Linetype_Struct*)IN_Struct)->pitch[i]);
				pitchstring += tempstring;
			}
			m_FeatureData.EntityData.Format("(\\'%s\\','%d','(%s)')",
				((Userdefined_Linetype_Struct*)IN_Struct)->name,
				((Userdefined_Linetype_Struct*)IN_Struct)->segment,
				pitchstring
				);
			break;
		case 6://width
			m_FeatureData.EntityName = "width_feature";
			m_FeatureData.EntityData.Format("('%f')",
				((Line_Width_Struct*)IN_Struct)->width);
			break;
		case 7://textfont
			m_FeatureData.EntityName = "text_font_feature";
			m_FeatureData.EntityData.Format("(\\'%s\\')",
				((Text_Font_Struct*)IN_Struct)->name);
			break;
    }
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableFeatureC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      Instance構造体Dataの取得(WRITE)
// 
//  ２．インターフェース
//      INSTANCEDATA* GetFeatureData( )
//
//	    引数: 型				引数名			    内容
//            なし                                      
//      
//	    復帰値	
//            INSTANCEDATA*		InstanceData構造体のアドレス
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
INSTANCEDATA* SXFTableFeatureC::GetFeatureData( )
{
    return &m_FeatureData;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableFeatureC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      構造体Dataの取得(READ)
// 
//  ２．インターフェース
//      LPVOID GetStructData()
//
//	    引数: 型			  引数名			    内容
//            なし
//      
//	    復帰値	
//            正常時：構造体のアドレス
//			  異常時：NULL
//      
//
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
LPVOID SXFTableFeatureC::GetStructData()
{
    return m_StructData;
}

//------------------------------------------------------------
//	All Rights Reserved,(c) JACIC 2001
//	SXFTableFeatureC.cpp                    作成日：2000.04.10
//
//  １．機能概要
//      Layer Class Dataを設定して、構造体Data生成(WRITE)
// 
//  ２．インターフェース
//      void SetLayerFeature(SXFLayerC *IN_Layer)
//
//	    引数: 型			  引数名			    内容
//            SXFLayerC*      IN_Layer             Layer Classのアドレス                        
//      
//	    復帰値:	
//            なし
//      
//  ３．履歴
//	    履歴番号    日付け         担当者    概要
//      作成		2000.04.10     周
//
//------------------------------------------------------------
void SXFTableFeatureC::SetLayerFeature(SXFLayerC *IN_Layer)
{
	m_FeatureData.InstanceMode=SXF_IM_FT_TABLE;
	m_FeatureData.InstanceID = m_InstanceIDManager->GetNewInstanceID();
	m_FeatureData.EntityName = "layer_feature";
    m_FeatureData.EntityData.Format("(\\'%s\\','%d')",
		((SXFLayerC*)IN_Layer)->m_Name,
		((SXFLayerC*)IN_Layer)->m_LfFlag);
}
