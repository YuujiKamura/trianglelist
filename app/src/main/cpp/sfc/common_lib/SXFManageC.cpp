/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．クラス概要
	変換管理クラス

	２．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/

#include "stdafx.h"
#include <stdio.h>
#include <iostream>
#include <afxtempl.h>
#include "SXFManageC.h"
#include "SXF_EXT.h"
#include "SXFErrorMsgC.h"
#include "SXFPart21FileWriteC.h"
#include "SXFTableManagerC.h"
#include "SXFWriteManagerC.h"
#include "SXFInstanceIDC.h"
#include "SXFPart21FileReadC.h"
#include "SXFReadFeatureManagerC.h"
#include "SXFAPI_RevInfo.h"

#ifdef _DEBUG
#define new DEBUG_NEW
#undef THIS_FILE
static char THIS_FILE[] = __FILE__;
#endif

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	コンストラクタ

	２．インターフェース
	void SXFManageC::SXFManageC()
	引数:	型					引数名				内容
			なし
	復帰値	なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/
SXFManageC::SXFManageC()
{
	m_Assem_type = -1;
	m_WriteFile = NULL;
	m_TableManager = NULL;
	m_WriteManager = NULL;
	m_ReadFile = NULL;
	m_ReadFeatureManager = NULL;
	m_InstanceIDManager = NULL;

}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	デストラクタ

	２．インターフェース
	void SXFManageC::~SXFManageC()
	引数:	型					引数名				内容
			なし
	復帰値	なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/
SXFManageC::~SXFManageC()
{
	if (m_WriteFile != NULL)
		delete m_WriteFile;
	if (m_WriteManager != NULL)
		delete m_WriteManager;
	if (m_ReadFile != NULL)
		delete m_ReadFile;
	if (m_ReadFeatureManager != NULL)
		delete m_ReadFeatureManager;
	if (m_InstanceIDManager != NULL)
		delete m_InstanceIDManager;
	if (m_TableManager != NULL)
		delete m_TableManager;
	if(gl_log_file!=NULL){
		if(gl_log_file->is_open()){
			gl_log_file->close();
		}
		delete gl_log_file;
	}
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	ファイルをオープンする

	２．インターフェース
	int Open(char IN_file_name[257],
					int IN_rw_flg, 
					double IN_tolerance1,
					double IN_tolerance2,
					double IN_divide, 
					int IN_level, 
					int IN_mode, 
					char IN_author[257], 
					char IN_org[257],
					char IN_translator_name[257],
					char OUT_version[257]);
	引数:	型		引数名					内容
			char	IN_file_name[]			ファイル名
			int		IN_rw_flg				読み書きフラグ(0:read 1:write)
			double	IN_tolerance1			許容誤差
			double	IN_tolerance2			角度用許容誤差
			double	IN_divide				ダウンコンバート用許容誤差(未使用)
			int		IN_level				レベル(1or2)
			int		IN_mode					モード (0:併用
													1:フィーチャコメント
													2:AP202）
			char	INOUT_author[]			ファイル作成者
			char	INOUT_org[]				作成者所属
			char	INOUT_translator_name[]	トランスレータ名
			char	OUT_version[]			共通ライブラリバージョン
	復帰値	正常時：０
			異常時：エラー番号
			
	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/
int SXFManageC::Open(char IN_file_name[257],
						int IN_rw_flg, 
						double IN_tolerance1,
						double IN_tolerance2,
						double IN_divide, 
						int IN_level, 
						int IN_mode, 
						char INOUT_author[257], 
						char INOUT_org[257],
						char INOUT_translator_name[257],
						char OUT_version[257])
{

	int ret;
	/////////////////////////////
	//エラーログファイルオープン
	/////////////////////////////
	gl_log_file = new ofstream("out.log",ios::out);
	if(gl_log_file->fail()){
		delete gl_log_file;
		gl_log_file = NULL;
		cerr << "Error opening out.log" << endl;
		return -1;
	}

	////////////////////////////
	//引数のチェック
	////////////////////////////
	if ((ret = Param_Check(IN_rw_flg,IN_level,IN_mode)) < 0)
		return ret;

	////////////////////////////
	//		READ
	////////////////////////////
	if (IN_rw_flg == 0){//read
		//共通情報登録
		m_Info.Set_Read_Come(IN_file_name,
								IN_rw_flg,
								IN_tolerance1,
								IN_tolerance2,
								IN_divide,
								IN_level,
								IN_mode);
		//Read管理オブジェクトの生成
		m_ReadFeatureManager = new SXFReadFeatureManagerC();
		//テーブル管理オブジェクトの生成
		m_TableManager = new SXFTableManagerC(&m_Info);
		//ファイル入力オブジェクト生成
		m_ReadFile = new SXFPart21FileReadC(m_TableManager,
											m_ReadFeatureManager,
											&m_Info);		
		//ファイルのオープン
		ret = m_ReadFile->OpenPart21ReadFile(IN_file_name);
			if (ret < 0){
				gl_log_file->close();
				delete gl_log_file;
				gl_log_file = NULL;
				return ret;
			}
        /////////////////////////////////////////
		//ヘッダ情報取得
		/////////////////////////////////////////
		int FileLevel = -1;
		int FileMode = -1;
		char FileName[257];
		CString	strOutVersion;
		m_ReadFile->GetHeadData(&FileLevel,
									&FileMode,
									FileName,
									INOUT_author,
									INOUT_org,
									strOutVersion.GetBuffer(257),
									INOUT_translator_name);
		strOutVersion.ReleaseBuffer();
		//ファイルバージョンの取得
		int pos = strOutVersion.Find("$$");
		if (pos == -1)
		{
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(OUT_version, (LPCTSTR)strOutVersion);
			// バッファサイズは仕様書より取得
			strcpy_s(OUT_version, 257, (LPCTSTR)strOutVersion);
			gl_FileVersion.Empty();
		}
		else
		{
			//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
			//strcpy(OUT_version, strOutVersion.Left(pos));
			// バッファサイズは仕様書より取得
			strcpy_s(OUT_version, 257, strOutVersion.Left(pos));
			gl_FileVersion = strOutVersion.Mid(pos + 2);
		}
		/*ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄにﾃｰﾌﾞﾙ管理ｵﾌﾞｼﾞｪｸﾄのｱﾄﾞﾚｽを渡す*/
		m_rulecheck.m_TableManager = m_TableManager;
		/*ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄにReadFeature管理ｵﾌﾞｼﾞｪｸﾄのｱﾄﾞﾚｽを渡す*/
		m_rulecheck.m_ReadFeatureManager = m_ReadFeatureManager;
		/*ﾙｰﾙﾁｪｯｸｵﾌﾞｼﾞｪｸﾄに共通情報管理のｱﾄﾞﾚｽを渡す*/
		m_rulecheck.m_ComInfo = &m_Info;

	}
	////////////////////////////
	//		WRITE
	////////////////////////////
	else if (IN_rw_flg == 1){//write
		//共通情報登録
		m_Info.Set_Write_Come(IN_file_name,
								IN_rw_flg,
								IN_tolerance1,
								IN_tolerance2,
								IN_divide,
								IN_level,
								IN_mode,
								INOUT_author,
								INOUT_org,
								INOUT_translator_name);
		//ファイルオープン
		m_WriteFile = new SXFPart21FileWriteC();
		ret = m_WriteFile->OpenPart21WriteFile(IN_file_name);
		if (ret < 0){
			gl_log_file->close();
			delete gl_log_file;
			gl_log_file = NULL;
			return ret;
		}
		////////////////////////////
		//バージョンの設定
		////////////////////////////
		CString	strVersion(SCADEC_API_RevInfo);
		if (!gl_FileVersion.IsEmpty())
		{
			//ファイルバージョン付加
			strVersion += "$$";
			strVersion += gl_FileVersion;
		}
		char Version[257];
//		strcpy(Version,(LPCTSTR)strVersion);	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
		strcpy_s(Version,_countof(Version),(LPCTSTR)strVersion);
		//////////////
		//ヘッダの出力
		m_WriteFile->SetHeadSection(2,		/** レベル2固定 **/
										1,	/** ﾌｨｰﾁｬｺﾒﾝﾄﾓｰﾄﾞ固定 **/
										IN_file_name,
										INOUT_author,
										INOUT_org,
										Version,
										INOUT_translator_name);
		ret = m_WriteFile->WriteHeadSection();
		if (ret < 0){
			gl_log_file->close();
			delete gl_log_file;
			gl_log_file = NULL;
			return ret;
		}
		//////
		//インスタンスＩＤオブジェクト生成
		m_InstanceIDManager = new SXFInstanceIDC();

		//テーブル管理オブジェクトの生成
		m_TableManager = new SXFTableManagerC(&m_Info,
												m_WriteFile,
												m_InstanceIDManager);
		//Write管理オブジェクトの生成
		m_WriteManager = new SXFWriteManagerC(m_WriteFile,m_InstanceIDManager);
		//ルールチェックオブジェクトにテーブル管理オブジェクトのアドレスを渡す
		m_rulecheck.m_TableManager = m_TableManager;
		//ルールチェックオブジェクトにWrite管理オブジェクトのアドレスを渡す
		m_rulecheck.m_WriteManager = m_WriteManager;
		//ルールチェックオブジェクトに共通情報管理のアドレスを渡す
		m_rulecheck.m_ComInfo = &m_Info;
		//エラーメッセージ用インスタンスIDの登録
		//WRITE時はデフォルトで"-"
		m_rulecheck.m_InstanceID = "-";

	}
	return 0;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	ファイルをクローズする

	２．インターフェース
	int Close()

	引数:	型				引数名			内容
			なし

	復帰値	正常時：０
			異常時：エラー番号

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/
int SXFManageC::Close()
{
	int flg,code,error_code;
	m_Info.Get_RWFlag(flg);
	code = 0;
	/** READ **/
	if (flg == 0){
		//ファイルクローズ
		m_ReadFile->ClosePart21ReadFile();
	}
	/** WRITE **/
	else if (flg == 1){
		//SHEET Mapping
		code = m_WriteManager->CloseAssembly();
		//LAYER Mapping
		m_TableManager->LayerMapping();
		//ファイルクローズ
		error_code = m_WriteFile->ClosePart21WriteFile();
		/* ﾌｧｲﾙｸﾛｰｽﾞ失敗 */
		if (error_code)
			code = error_code;
	}
	/** エラーログファイルクローズ **/
	gl_log_file->close();
	delete gl_log_file;
	gl_log_file = NULL;

	return code;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	オープン時に渡された引数(読み書きフラグ,レベル,モード)のチェックを行う

	２．インターフェース
	int Param_Check(int IN_rw_flg,int IN_level, int IN_mode)
	引数:	型				引数名			内容
			int				IN_rw_flg		読み書きフラグ
			int				IN_level		レベル
			int				IN_mode			モード

	復帰値	正常時：０
			異常時：エラー番号
			
	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/
int SXFManageC::Param_Check(int IN_rw_flg,int IN_level, int IN_mode)
{
	//読み書きフラグのチェック
	if ((IN_rw_flg != 0) && (IN_rw_flg != 1)){
		SXFErrorMsgC::RecordError(SXFRWFLAG,"",1,IN_rw_flg);
		return SXFRWFLAG;
	}
	//レベルのチェック
	switch (IN_level){
		/** 1のときワーニングとして処理続行 **/
		case 1:
// MOD(S)	メッセージ修正	K.Naono	04.03.05
//			SXFErrorMsgC::RecordError(SXFWARNINGLEVEL,"",1,IN_level);
			SXFErrorMsgC::RecordError2(SXFWARNINGLEVEL,"",1,IN_level);
// MOD(E)	メッセージ修正	K.Naono	04.03.05
			break;
		case 2:
			break;
		/** 1,2以外のときエラー処理 **/
		default:
			SXFErrorMsgC::RecordError(SXFNOLEVEL,"",1,IN_level);
			return SXFNOLEVEL;
	}
	//モードのチェック
	switch (IN_mode){
		/** ０，２のときワーニングとして処理続行 **/
		case 0:
		case 2:
// MOD(S)	メッセージ修正	K.Naono	04.03.05
//			SXFErrorMsgC::RecordError(SXFWARNINGMODE,"",1,IN_mode);
			SXFErrorMsgC::RecordError2(SXFWARNINGMODE,"",1,IN_mode);
// MOD(E)	メッセージ修正	K.Naono	04.03.05
			break;
		case 1:
			break;
		/** ０，１，２以外のときエラー処理 **/
		default:
			SXFErrorMsgC::RecordError(SXFNOMODE,"",1,IN_mode);
			return SXFNOMODE;
	}
	return 0;
	
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	定義テーブルフィーチャをファイルに出力する

	２．インターフェース
	int Write_table(int IN_table_type, void* IN_str)

	引数:	型				引数名				内容
			int				IN_table_type		定義テーブル型
													1:レイヤ
													2:既定義色
													3:ユーザ定義色
													4:既定義線種
													5:ユーザ定義線種
													6:線幅
													7:文字フォント
			void*			IN_str				構造体

	復帰値	正常時：コード
			異常時：エラー番号
			
	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/
int SXFManageC::Write_table(int IN_table_type, void* IN_str)
{
	int error_code,code;
	//ルールチェックオブジェクト呼び出し
	error_code = m_rulecheck.TableRuleCheck(IN_table_type,IN_str);
	if (error_code < 0)
		return error_code;
	//定義テーブル管理呼出し
	code = m_TableManager->RegisterStructTable(IN_table_type,IN_str);
	return code;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	アセンブリ要素フィーチャをファイルに出力する
	
	２．インターフェース
	int Write_assembly_name(int IN_Assem_type, void *IN_Struct)
	引数:	型				引数名				内容
			int				IN_Assem_type		アセンブリ型
													1:用紙
													2:複合図形
													3:複合曲線
			void*			IN_Struct			構造体

	復帰値	正常時：共通ライブラリがふる番号
			異常時：エラー番号
			
	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/
int SXFManageC::Write_assembly_name(int IN_Assem_type, void *IN_Struct)
{
	int error_code,Assem_code;
	//ルールチェックオブジェクト呼び出し
	error_code = m_rulecheck.AssemblyRuleCheck(IN_Assem_type,IN_Struct);
	if (error_code < 0){
		m_WriteManager->ErrorAssembly();
		m_Assem_type = -1;
		return error_code ;
	}
	Assem_code = m_WriteManager->RegisterAssembly(IN_Assem_type,IN_Struct);
	//今開かれているアセンブリタイプ、エラーのときは-１
	m_Assem_type = m_WriteManager->GetAssemType();
	return Assem_code;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	一般要素フィーチャをファイルに出力する

	２．インターフェース
	int Write_next_feature(char *IN_FeatureType, void *IN_Struct)

	引数:	型				引数名				内容
			char*			IN_FeatureType		フィーチャ型
			void*			IN_Struct			構造体
	
	 復帰値	正常時：インスタンス番号
			異常時：エラー番号
			
	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/
int SXFManageC::Write_next_feature(char *IN_FeatureType, void *IN_Struct)
{
	int error_code;
	//ルールチェックオブジェクト呼び出し
	error_code = m_rulecheck.FeatureRuleCheck(m_Assem_type,
												IN_FeatureType,
												IN_Struct);
	if (error_code < 0)
		return error_code ;
	error_code = m_WriteManager->RegisterFeature(IN_FeatureType,IN_Struct);
	return error_code;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	定義テーブル要素をよんで、構造体を返す

	２．インターフェース
	int Read_table(int IN_TableType, LPVOID &OUT_Struct, int &OUT_EndFlag)

	引数:	型				引数名			内容
			int				IN_TableType	テーブル型
			LPVOID&			OUT_Struct		構造体
			int&			OUT_EndFlag		終了フラグ（０：継続、１：後続なし）
	
	 復帰値	正常時：コード
			異常時：エラー番号
			
	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/
int SXFManageC::Read_table(int IN_TableType,LPVOID OUT_Struct,int* OUT_EndFlag)
{	
	int code,error_code;
	//構造体の取りだし
	code = m_TableManager->GetTableElement(IN_TableType,OUT_Struct,OUT_EndFlag);
	if (code < 0)
		return code;
	//ルールチェック
	error_code = m_rulecheck.TableRuleCheck(IN_TableType,OUT_Struct);
	if (error_code < 0)
		return error_code;
	return code;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	アセンブリ要素を読みこんで、構造体を返す

	２．インターフェース
	int Read_assembly_name(int IN_Type, LPVOID &OUT_Struct, int &OUT_EndFlag)

	引数:	型				引数名			内容
			int				IN_Type			アセンブリ型
			LPVOID&			OUT_Struct		構造体
			int&			OUT_EndFlag		終了フラグ（０：継続、１：後続なし）

	復帰値	正常時：共通ライブラリがふる番号
			異常時：エラー番号
			
	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。

**/
int SXFManageC::Read_assembly_name(int IN_Type, 
								   LPVOID &OUT_Struct, 
								   int *OUT_EndFlag)
{
	int InstanceID,returnCode,error_code;
	m_Assem_type = -1;
	returnCode = m_ReadFeatureManager->GetNextAssembly(IN_Type,
													OUT_Struct,
													OUT_EndFlag,
													InstanceID);
	if (returnCode < 0)
		return returnCode;
	//エラーメッセージ用インスタンスID
	m_rulecheck.m_InstanceID.Format("#%d",InstanceID);	
	//ルールチェック
	error_code = m_rulecheck.AssemblyRuleCheck(IN_Type,OUT_Struct);
	if (error_code < 0){
// MOD(S)	メモリリーク不具合対応	K.Naono	04.02.03
//		delete OUT_Struct;
		Delete_assembly(IN_Type, OUT_Struct);
		OUT_Struct = NULL;
// MOD(E)	メモリリーク不具合対応	K.Naono	04.02.03
		return error_code;
	}
	///現在のアセンブリ型　フィーチャ要素のルールチェックに使用
	m_Assem_type = IN_Type;
	return returnCode;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2000.3.29

	１．機能概要
	一般フィーチャ要素を読みこんで、構造体を返す

	２．インターフェース
	int Read_next_feature(char* OUT_Type, size_t TypeSize, LPVOID &OUT_Struct, int &OUT_EndFlag)
	引数:	型				引数名				内容
			char*			OUT_Type			フィーチャ型
			size_t			TypeSize			フィーチャ型のバッファサイズ
			LPVOID&			OUT_Struct			構造体
			int&			OUT_EndFlag			終了フラグ
												（０：継続、１：後続なし）
	復帰値	正常時：インスタンス番号
			異常時：エラー番号
			
	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2000.3.29			宮園(FQS)			なし。
	更新			2015.04.27			T.Matsushima		JPCERT/CC 指摘 脆弱性対応

**/
int SXFManageC::Read_next_feature(char* OUT_Type, 
								  size_t TypeSize,		// バッファサイズを渡すように変更
								  LPVOID &OUT_Struct, 
								  int *OUT_EndFlag)
{
	int returnID,error_code;
	returnID = m_ReadFeatureManager->GetNextFeature(OUT_Type,
												TypeSize,	//	JPCERT/CC 指摘 脆弱性対応	T.Matsushima	2015.04.27
												OUT_Struct,
												OUT_EndFlag);
	if (returnID < 0)
		return returnID;
	//エラーメッセージ用インスタンスID
	m_rulecheck.m_InstanceID.Format("#%d",returnID);
	//ルールチェック
	error_code = m_rulecheck.FeatureRuleCheck(m_Assem_type,
											OUT_Type,
											OUT_Struct);
	if (error_code < 0){
// MOD(S)	メモリリーク不具合対応	K.Naono	04.02.03
//		delete OUT_Struct;
		Delete_feature(OUT_Type, OUT_Struct) ;
		OUT_Struct = NULL ;
// MOD(E)	メモリリーク不具合対応	K.Naono	04.02.03
		return error_code;
	}
	return returnID;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2001.2.23

	１．機能概要
		フィーチャ構造体の領域解放

	２．インターフェース
	void SXFManageC::Delete_feature(char* feature_name,void *& f_struct)

	引数:	型			引数名			内容
			char*		feature_name	フィーチャ名
			void*&		f_struct		フィーチャ構造体のアドレス

	復帰値	なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2001.2.23			宮園(FQS)			なし。

**/
void SXFManageC::Delete_feature(char* feature_name,void *& f_struct)
{
	if (f_struct)
		if (strcmp(feature_name,"POLYLINE") == 0)
			delete (Polyline_Struct*)f_struct;
		else if (strcmp(feature_name,"SPLINE") == 0)
			delete (Spline_Struct*)f_struct;
		else if (strcmp(feature_name,"CLOTHOID") == 0)
			delete (Clothoid_Struct*)f_struct;
		else if (strcmp(feature_name,"LABEL") == 0)
			delete (Label_Struct*)f_struct;
		else if (strcmp(feature_name,"BALLOON") == 0)
			delete (Balloon_Struct*)f_struct;
		else if (strcmp(feature_name,"FILL_AREA_STYLE_HATCHING") == 0)
			delete (Fill_area_style_hatching_Struct*)f_struct;
		else if (strcmp(feature_name,"EXTERNALLY_DEFINED_HATCH") == 0)
			delete (Externally_Defined_Hatch_Struct*)f_struct;
		else if (strcmp(feature_name,"FILL_AREA_STYLE_COLOUR") == 0)
			delete (Fill_area_style_colour_Struct*)f_struct;
		else if (strcmp(feature_name,"FILL_AREA_STYLE_TILES") == 0)
			delete (Fill_area_style_tiles_Struct*)f_struct;
		else 
			delete f_struct;
	f_struct = NULL;
}

/**
	All Rights Reserved,(c) JACIC 2001
	SXFManageC.cpp								作成日：2001.2.23

	１．機能概要
		アセンブリ構造体の領域解放

	２．インターフェース
	void SXFManageC::Delete_assembly(int assem_type,void *& f_struct)

	引数:	型			引数名		内容
			char*		assem_type	アセンブリ型
										１：用紙
										２：複合図形
										３：複合曲線
			void*&		f_struct	フィーチャ構造体のアドレス

	復帰値	なし

	３．履歴
	履歴番号		日付け				担当者				概要
	作成			2001.3.28			宮園(FQS)			なし。

**/
void SXFManageC::Delete_assembly(int assem_type,void *& f_struct)
{
	if (f_struct){
		switch(assem_type){
			case 1:
			case 2:
			case 3:
				delete f_struct;
				break;
			default:
				SXFErrorMsgC::RecordError(SXFASSEMTYPEERR,"",1,assem_type);
				break;
		}
	}
	f_struct = NULL;
}
