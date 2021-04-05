/**
	All Rights Reserved,(c) JACIC 2001
	SXFRuleCheckC.cpp								作成日：2000.4.19

	１．クラス概要
	フィーチャのルールチェックを行う

	２．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.19			宮園(FQS)			なし。

**/

#include "stdafx.h"
#include "SXFRuleCheckC.h"
#include "SXFStruct.h"
#include "SXFErrorMsgC.h"
#include "SXFTableManagerC.h"
#include "SXFWriteManagerC.h"
#include "SXFComInfoC.h"
#include "SXFReadFeatureManagerC.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/**
	All Rights Reserved,(c) JACIC 2001
	SXFRuleCheckC.cpp								作成日：2000.4.19

	１．機能概要
		コンストラクタ

	２．インターフェース
		void	 SXFRuleCheckC::SXFRuleCheckC()
		
	引数	：	型		引数名				内容
		なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.19			宮園(FQS)			なし。

**/
SXFRuleCheckC::SXFRuleCheckC()
{
	m_ReadFeatureManager = NULL;
	m_WriteManager = NULL;
	m_TableManager = NULL;
	m_ComInfo = NULL;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFRuleCheckC.cpp								作成日：2000.4.19

	１．機能概要
		デストラクタ

	２．インターフェース
		void	 SXFRuleCheckC::S~XFRuleCheckC()
		
	引数	：	型		引数名				内容
		なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.19			宮園(FQS)			なし。

**/
SXFRuleCheckC::~SXFRuleCheckC()
{

}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFRuleCheckC.cpp								作成日：2000.4.19

	１．機能概要
		定義テーブル要素のルールチェック

	２．インターフェース
		int	 SXFRuleCheckC::TableRuleCheck(int IN_Type, LPVOID IN_Struct)
		
	引数	：	型		引数名		内容
				int		IN_Type		テーブル型
				LPVOID	IN_Struct	定義テーブルフィーチャ構造体

		正常終了	：0
		異常終了	：エラーコード

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.19			宮園(FQS)			なし。

**/
int SXFRuleCheckC::TableRuleCheck(int IN_Type, LPVOID IN_Struct)
{
	int error_code = 0;

	switch(IN_Type){
	//レイヤ
	case 1:
		error_code = m_Layer_check.Feature_rule_check((Layer_Struct*)IN_Struct,
														m_TableManager,
														m_ComInfo);
		break;
	//既定義色
	case 2:
		error_code = m_PreColour_check.Feature_rule_check((Predefined_Colour_Struct*)IN_Struct,
															m_TableManager,
															m_ComInfo);
		break;
	//ユーザ定義色
	case 3:
		error_code = m_Usercolor_check.Feature_rule_check((Userdefined_Colour_Struct*)IN_Struct);
		break;
	//既定義線種
	case 4:
		error_code = m_PreType_check.Feature_rule_check((Predefined_Linetype_Struct*)IN_Struct,
															m_TableManager,
															m_ComInfo);
		break;
	//ユーザ定義線種
	case 5:
		error_code = m_Usertype_check.Feature_rule_check((Userdefined_Linetype_Struct*)IN_Struct,
															m_TableManager,
															m_ComInfo);
		break;
	//線幅
	case 6:
		error_code = m_Width_check.Feature_rule_check((Line_Width_Struct*)IN_Struct,
															m_TableManager,
															m_ComInfo);
		break;
	//文字フォント
	case 7:
		error_code = m_TextFont_check.Feature_rule_checkC((Text_Font_Struct*)IN_Struct,
															m_TableManager,
															m_ComInfo);
		break;
	default:
		SXFErrorMsgC::RecordError(SXFTABLETYPEERR,"",1,IN_Type);
		return SXFTABLETYPEERR;
	}
	return error_code;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFRuleCheckC.cpp								作成日：2000.4.19

	１．機能概要
		アセンブリ要素のルールチェック

	２．インターフェース
		int	 SXFRuleCheckC::AssemblyRuleCheck(int IN_Type, LPVOID IN_Struct)
		
	引数	：	型		引数名		内容
				int		IN_Type		アセンブリ型
				LPVOID	IN_Struct	アセンブリ要素フィーチャ構造体

		正常終了	：0
		異常終了	：エラーコード

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.19			宮園(FQS)			なし。

**/
int SXFRuleCheckC::AssemblyRuleCheck(int IN_Type, LPVOID IN_Struct)
{
	int error_code = 0;
	int RWFlag = -1;
	m_ComInfo->Get_RWFlag(RWFlag);

	switch(IN_Type){
		/** 用紙 **/
		case 1:
			//WRITE(Readではすでにﾁｪｯｸ済み)
			if (RWFlag == 1){
				error_code = m_Sheet_check.Feature_rule_check((Sheet_Struct*)IN_Struct);
			}
			m_SfigFlag = -1;	/* 複合図形種別ﾌﾗｸﾞOFF */
			break;
		/** 複合図形 **/
		case 2:
			/* 複合図形種別ﾌﾗｸﾞ */
			m_SfigFlag = ((Sfigorg_Struct*)IN_Struct)->flag;
			//WRITE(Readではすでにﾁｪｯｸ済み)
			if (RWFlag == 1){
				//同じ名前ですでに定義されている
				if (m_WriteManager->SfigNameCheck(((Sfigorg_Struct*)IN_Struct)->name)){
					SXFErrorMsgC::RecordError(SXFSAMENAME,
										"SFIG_ORG",
										1,
										((Sfigorg_Struct*)IN_Struct)->name);
					return SXFSAMENAME;
				}
				error_code = m_Sfigorg_check.Feature_rule_check(
														(Sfigorg_Struct*)IN_Struct,
														m_InstanceID);
			}
			break;
		/** 複合曲線 **/
		case 3:
			error_code = m_Ccurveorg_check.Feature_rule_check(
													(Ccurve_Org_Struct*)IN_Struct,
													m_TableManager,
													m_InstanceID);
			m_SfigFlag = -1;	/* 複合図形種別ﾌﾗｸﾞOFF */
			break;
		default:
			SXFErrorMsgC::RecordError(SXFASSEMTYPEERR,"",1,IN_Type);
			return SXFASSEMTYPEERR;
	}
	return error_code;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFRuleCheckC.cpp								作成日：2000.4.19

	１．機能概要
		一般要素のルールチェック

	２．インターフェース
		int	 SXFRuleCheckC::FeatureRuleCheck(int IN_AssemType,
											char* IN_Type,
											LPVOID IN_Struct)
		
	引数	：	型		引数名			内容
				int		IN_AssemType	アセンブリ型
				char*	IN_Type			一般要素名
				LPVOID	IN_Struct		一般要素フィーチャ構造体

	復帰値：
		正常終了	：0
		異常終了	：エラーコード

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.4.19			宮園(FQS)			なし。

**/
int SXFRuleCheckC::FeatureRuleCheck(int IN_AssemType,char *IN_Type, LPVOID IN_Struct)
{
	int error_code = 0;
	int SfigLocType,LocFlg;
	int rw_flg = -1;
	m_ComInfo->Get_RWFlag(rw_flg);
	//Assembly not open
	if (IN_AssemType == -1){
		SXFErrorMsgC::RecordError(SXFNOTASSEMBLY,IN_Type,0);
		return SXFNOTASSEMBLY;
	}
	/** 点マーカ **/
	if (strcmp(IN_Type,"POINT_MARKER") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_Point_Marker_check.Feature_rule_check(
													(Point_Marker_Struct*)IN_Struct,
													m_TableManager,
													m_InstanceID);
	}

//	ADD(S)	Ver3.0対応	K.Naono	02.12.03
	/** 図面表題欄 **/
	else if (strcmp(IN_Type,"ATTRIBUTE") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_Attribute_check.Feature_rule_check(
										(Attribute_Struct*)IN_Struct,
										m_TableManager,
										m_InstanceID) ;
		if( IN_AssemType != 2 ) {
			return error_code ;
		}

		/** 用紙または、部分図以外には配置できない **/
		if( m_SfigFlag != 1 && m_SfigFlag != 2) {
			TRACE0("用紙または、部分図以外には配置できません...\n") ;
			SXFErrorMsgC::RecordError(SXFNOTATTRIBUTE,IN_Type,1,IN_Type) ;
			return SXFNOTATTRIBUTE ;
		}
	}
//	ADD(E)	Ver3.0対応	K.Naono	02.12.03

	/** 線分 **/
	else if (strcmp(IN_Type,"LINE") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_Line_check.Feature_rule_check((Line_Struct*)IN_Struct,
													m_TableManager,
													m_InstanceID,
													m_ComInfo);
	}
	/** 文字要素 **/
	else if (strcmp(IN_Type,"TEXT") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_Text_String_check.Feature_rule_check(
												(Text_Struct*)IN_Struct,
												m_TableManager,
												m_InstanceID);
	}
	/** 円弧 **/
	else if (strcmp(IN_Type,"ARC") == 0){
		error_code = m_Arc_check.Feature_rule_check((Arc_Struct*)IN_Struct,
														m_TableManager,
														IN_AssemType,
														m_InstanceID,
														m_ComInfo);
	}
	/** 円 **/
	else if (strcmp(IN_Type,"CIRCLE") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_Circle_check.Feature_rule_check((Circle_Struct*)IN_Struct,
															m_TableManager,
															m_InstanceID);
	}
	/** 楕円 **/
	else if (strcmp(IN_Type,"ELLIPSE") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_Ellipse_check.Feature_rule_check(
												(Ellipse_Struct*)IN_Struct,
												m_TableManager,
												m_InstanceID);
	}
	/** 楕円弧 **/
	else if (strcmp(IN_Type,"ELLIPSE_ARC") == 0){
		error_code = m_Ellipse_Arc_check.Feature_rule_check(
												(Ellipse_Arc_Struct*)IN_Struct,
												m_TableManager,
												IN_AssemType,
												m_InstanceID,
												m_ComInfo);
	}
	/** 折線 **/
	else if (strcmp(IN_Type,"POLYLINE") == 0){
		error_code = m_Polyline_check.Feature_rule_check(
												(Polyline_Struct*)IN_Struct,
												m_TableManager,
												IN_AssemType,
												m_InstanceID,
												m_ComInfo);
	}
	/** スプライン **/
	else if (strcmp(IN_Type,"SPLINE") == 0){
		error_code = m_Spline_check.Feature_rule_check((Spline_Struct*)IN_Struct,
															m_TableManager,
															IN_AssemType,
															m_InstanceID);
	}
	/** クロソイド **/
	else if (strcmp(IN_Type,"CLOTHOID") == 0){
		error_code = m_Clothoid_check.Feature_rule_check((Clothoid_Struct*)IN_Struct,
															m_TableManager,
															m_InstanceID,
															m_ComInfo);
	}
	/** 直線寸法 **/
	else if (strcmp(IN_Type,"LINEAR_DIMENSION") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_LinearDim_check.Feature_rule_check(
												(LinearDim_Struct*)IN_Struct,
												m_TableManager,
												m_InstanceID,
												m_ComInfo);
	}
	/** 弧長寸法 **/
	else if (strcmp(IN_Type,"CURVE_DIMENSION") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_CurveDim_check.Feature_rule_check(
												(CurveDim_Struct*)IN_Struct,
												m_TableManager,
												m_InstanceID,
												m_ComInfo);
	}
	/** 角度寸法 **/
	else if (strcmp(IN_Type,"ANGULAR_DIMENSION") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_AngularDim_check.Feature_rule_check(
												(AngularDim_Struct*)IN_Struct,
												m_TableManager,
												m_InstanceID,
												m_ComInfo);
	}
	/** 半径寸法 **/
	else if (strcmp(IN_Type,"RADIUS_DIMENSION") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_RadiusDim_check.Feature_rule_check(
												(RadiusDim_Struct*)IN_Struct,
												m_TableManager,
												m_InstanceID,
												m_ComInfo);
	}
	/** 直径寸法 **/
	else if (strcmp(IN_Type,"DIAMETER_DIMENSION") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_DiameterDim_check.Feature_rule_check(
												(DiameterDim_Struct*)IN_Struct,
												m_TableManager,
												m_InstanceID,
												m_ComInfo);
	}
	/** 引出し線 **/
	else if (strcmp(IN_Type,"LABEL") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_Label_check.Feature_rule_check(
												(Label_Struct*)IN_Struct,
												m_TableManager,
												m_InstanceID,
												m_ComInfo);
	}
	/** バルーン **/
	else if (strcmp(IN_Type,"BALLOON") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_Balloon_check.Feature_rule_check(
												(Balloon_Struct*)IN_Struct,
												m_TableManager,
												m_InstanceID,
												m_ComInfo);
	}
	/** 既定義シンボル **/
	else if (strcmp(IN_Type,"EXTERNALLY_DEFINED_SYMBOL") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_ExSymbol_check.Feature_rule_check(
								(Externally_Defined_Symbol_Struct*)IN_Struct,
								m_TableManager,
								m_InstanceID);
	}
	/** 複合図形配置 **/
	else if (strcmp(IN_Type,"SFIG_LOCATE") == 0){
		//複合曲線には配置できない。
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		//WRITE
		if (rw_flg == 1){
			/** 複合図形種別、配置フラグ取得 **/
			if (!m_WriteManager->GetSfigFlg(((Sfigloc_Struct*)IN_Struct)->name,
											&SfigLocType,&LocFlg)){
				/** 定義されていない **/
				SXFErrorMsgC::RecordError(SXFNOTSETCFIG,
											IN_Type,
											3,
											"-",
											((Sfigloc_Struct*)IN_Struct)->name,
											IN_Type);
				return SXFNOTSETCFIG;
			}
			/** 部分図配置 **/
			if (SfigLocType == 1 || SfigLocType == 2){
				/** 既に配置済み(複数配置不可) **/
				if (LocFlg == 1){
					SXFErrorMsgC::RecordError(SXFSFIGLOCONE,
											IN_Type,
											3,
											m_InstanceID,
											((Sfigloc_Struct*)IN_Struct)->name,
											IN_Type);
					return SXFSFIGLOCONE;
				}
				/** 部分図上に部分図は配置できない **/
				if (m_SfigFlag == 1 || m_SfigFlag == 2){
					SXFErrorMsgC::RecordError(SXFSFIGERROR,
												IN_Type,
												2,
												"-",
												IN_Type);
					return SXFSFIGERROR;
				}
				/** 作図グループ上に部分図は配置できない */
				else if (m_SfigFlag == 3){
					SXFErrorMsgC::RecordError(SXFGROUPERROR,
												IN_Type,
												2,
												"-",
												IN_Type);
					return SXFSFIGERROR;
				}
				/** 作図部品には部分図を配置できない **/
				else if (m_SfigFlag == 4){
					SXFErrorMsgC::RecordError(SXFPARTERROR,
												IN_Type,
												1,
												IN_Type);
					return SXFPARTERROR;
				}
			}
			/** グループ配置 **/
			else if (SfigLocType == 3){
				/** 既に配置済み(複数配置不可) **/
				if (LocFlg == 1){
					SXFErrorMsgC::RecordError(SXFSFIGLOCONE,
											IN_Type,
											3,
											m_InstanceID,
											((Sfigloc_Struct*)IN_Struct)->name,
											IN_Type);
					return SXFSFIGLOCONE;
				}
				/** 作図部品上にはグループを配置できない **/
				if (m_SfigFlag == 4){
					SXFErrorMsgC::RecordError(SXFPARTERROR,IN_Type,1,IN_Type);
					return SXFPARTERROR;
				}
			}
			error_code = m_Sfigloc_check.Feature_rule_check(
											(Sfigloc_Struct*)IN_Struct,
											m_TableManager,
											m_InstanceID);
			if (!error_code)
				/** 配置フラグON **/
				m_WriteManager->SetLocFlg(((Sfigloc_Struct*)IN_Struct)->name);
		}
		//Read
		else if (rw_flg == 0){
			//複合図形が設定されていない（テーブルにない）
			if (!m_ReadFeatureManager->GetSfigFlg(((Sfigloc_Struct*)IN_Struct)->name,
													&SfigLocType,
													&LocFlg)){
				SXFErrorMsgC::RecordError(SXFNOTSETCFIG,
											IN_Type,
											3,
											"-",
											((Sfigloc_Struct*)IN_Struct)->name,
											IN_Type);
				return SXFNOTSETCFIG;
			}
			/** 部分図配置 **/
			if (SfigLocType == 1 || SfigLocType == 2){
				/** 既に配置済み(複数配置不可) **/
				if (LocFlg == 1){
					SXFErrorMsgC::RecordError(SXFSFIGLOCONE,
												IN_Type,
												3,
												m_InstanceID,
												((Sfigloc_Struct*)IN_Struct)->name,
												IN_Type);
					return SXFSFIGLOCONE;
				}
				/** 部分図上に部分図は配置できない **/
				if (m_SfigFlag == 1 || m_SfigFlag == 2){
					SXFErrorMsgC::RecordError(SXFSFIGERROR,
												IN_Type,
												2,
												"-",
												IN_Type);
					return SXFSFIGERROR;
				}
				/** 作図グループ上に部分図は配置できない */
				else if (m_SfigFlag == 3){
					SXFErrorMsgC::RecordError(SXFGROUPERROR,
												IN_Type,
												2,
												"-",
												IN_Type);
					return SXFSFIGERROR;
				}
				/** 作図部品上には部分図を配置できない **/
				else if (m_SfigFlag == 4){
					SXFErrorMsgC::RecordError(SXFPARTERROR,
												IN_Type,
												1,
												IN_Type);
					return SXFPARTERROR;
				}
			}
			/** 作図グループ配置 **/
			else if (SfigLocType == 3){
				/** 既に配置済み(複数配置不可) **/
				if (LocFlg == 1){
					SXFErrorMsgC::RecordError(SXFSFIGLOCONE,
												IN_Type,
												3,
												m_InstanceID,
												((Sfigloc_Struct*)IN_Struct)->name,
												IN_Type);
					return SXFSFIGLOCONE;
				}
				/** 作図部品上にはグループを配置できない **/
				if (m_SfigFlag == 4){
					SXFErrorMsgC::RecordError(SXFPARTERROR,
												IN_Type,
												1,
												IN_Type);
					return SXFPARTERROR;
				}
			}
			error_code = m_Sfigloc_check.Feature_rule_check(
											(Sfigloc_Struct*)IN_Struct,
											m_TableManager,
											m_InstanceID);
			if (!error_code)
				/** 配置フラグON **/
				m_ReadFeatureManager->SetLocFlg(((Sfigloc_Struct*)IN_Struct)->name);
		}
	}
	/** 外部定義ハッチ **/
	else if (strcmp(IN_Type,"EXTERNALLY_DEFINED_HATCH") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_Exhatch_check.Feature_rule_check(
								(Externally_Defined_Hatch_Struct*)IN_Struct,
								m_TableManager,
								m_ReadFeatureManager,
								m_WriteManager,
								m_InstanceID,
								m_ComInfo);
	}
	/** ハッチング(塗り) **/
	else if (strcmp(IN_Type,"FILL_AREA_STYLE_COLOUR") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_FillColour_check.Feature_rule_check(
										(Fill_area_style_colour_Struct*)IN_Struct,
										m_TableManager,
										m_ReadFeatureManager,
										m_WriteManager,
										m_InstanceID,
										m_ComInfo);
	}
	/** ハッチング(ユーザ定義) **/
	else if (strcmp(IN_Type,"FILL_AREA_STYLE_HATCHING") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_FillHatching_check.Feature_rule_check(
										(Fill_area_style_hatching_Struct*)IN_Struct,
										m_TableManager,
										m_ReadFeatureManager,
										m_WriteManager,
										m_InstanceID,
										m_ComInfo);
	}
	/** ハッチング(パターン) **/
	else if (strcmp(IN_Type,"FILL_AREA_STYLE_TILES") == 0){
		/** 複合曲線上に配置できない **/
		if (IN_AssemType == 3){
			SXFErrorMsgC::RecordError(SXFNOTCURVE,IN_Type,1,IN_Type);
			return SXFNOTCURVE;
		}
		error_code = m_FillTiles_check.Feature_rule_check(
										(Fill_area_style_tiles_Struct*)IN_Struct,
										m_TableManager,
										m_ReadFeatureManager,
										m_WriteManager,
										m_InstanceID,
										m_ComInfo);
	}
	else{
		SXFErrorMsgC::RecordError(SXFFEATURETYPEERR,"",1,IN_Type);
		return SXFFEATURETYPEERR;
	}
	return error_code;

}

