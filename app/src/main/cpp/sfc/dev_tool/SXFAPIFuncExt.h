#ifndef _SXFAPIFUNCEXT_H_
#define _SXFAPIFUNCEXT_H_

#include <afx.h>

extern int WINAPI SXFdelete_assembly(int, void*&);
extern int WINAPI SXFdelete_feature(char*,void*&);
extern int WINAPI SXFopen_part21(char[257],int,double,double,double,int,int,char[257],char[257],char[257],char[257]);
extern int WINAPI SXFset_file_version_part21(char[257]);
extern int WINAPI SXFget_file_version_part21(char[257]);
extern int WINAPI SXFclose_part21();
extern int WINAPI SXFwrite_next_feature(char*, void*);
extern int WINAPI SXFread_assembly_name(int,void* &,int*);
extern int WINAPI SXFread_next_feature(char*,void* &,int*);
extern int WINAPI SXFwrite_assembly_name(int,void*);
extern int WINAPI SXFwrite_table(int,void*);
extern int WINAPI SXFread_table(int,void*,int*);
extern int WINAPI SXFPopMsg(char*&,int*,char* &);

#endif
