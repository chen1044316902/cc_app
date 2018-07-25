//package com.test;
//
//
//class Admin {
//	private String aid ;
//	private String password ;
//	private Role role ;
//	public Admin(String aid,String password) {
//		this.aid = aid ;
//		this.password = password ;
//	}public void setRole(Role role) {
//		this.role = role ;
//	}
//	public Role getRole() {
//		return this.role ;
//	}
//	public String getInfo() {
//		return "管理员编号："+ this.aid +",密码："+ this.password ;
//	}
//}
//class Role {
//	private int rid ;
//	private String title ;
//	private Admin admins [] ;
//	private Group groups [] ;
//	public Role(int rid,String title) {
//		this.rid = rid ;
//		this.title = title ;
//	}
//	public void setAdmins(Admin[] admins) {
//		this.admins = admins ;
//	}
//	public Admin [] getAdmins() {
//		return this.admins ;
//	}
//	public void setGroups(Group[] groups) {
//		this.groups = groups ;
//	}
//	public Group [] getGroup() {
//		return this.groups ;
//	}
//	public String getInfo() {
//		return "角色编号："+ this.rid +",名称："+ this.title ;
//	}
//}
//class Group {
//	private int gid ;
//	private String title ;
//	private Role roles [] ;
//	private Action actions [] ;
//	public Group(int gid,String title) {
//		this.gid = gid ;
//		this.title = title ;
//	}
//	public void setRoles(Role[] roles) {
//		this.roles = roles ;
//	}
//	public Role[] getRoles() {
//		return this.roles ;
//	}
//	public void setActions(Action[] actions) {
//		this.actions = actions ;
//	}
//	public Action[] getActions() {
//		return this.actions ; 
//	}
//	public String getInfo() {
//		return "权限组编号："+ this.gid+ ",名称："+ this.title ;
//	}
//}
//class Action {
//	private int aid ;
//	private String title ;
//	private String url ;
//	private	Group group ;
//	public Action(int aid,String title,String url) {
//		this.aid = aid ;
//		this.title = title ;
//		this.url = url ;
//	}
//	public void setGroup(Group group) {
//		this.group = group ;
//	}
//	public Group getGroup() {
//		return this.group ;
//	}
//	public String getInfo() {
//		return "权限编号："+this.aid +",名称："+ this.title +",路径："+ this.url ;
//	}
//}
//public class TestAdmin {
//	public static void main (String args[]) {
//		//第一步：设置完整关系
//		Admin a1 = new Admin("admin","hello") ;
//		Admin a2 = new Admin("guest","hello") ;
//		Admin a3 = new Admin("test","hello") ;
//		Role r1 = new Role(1,"系统管理员") ;
//		Role r2 = new Role(2,"信息管理员") ;
//		Group g1 = new Group(10,"信息管理") ;
//		Group g2 = new Group(11,"用户管理") ;
//		Group g3 = new Group(12,"数据管理") ;
//		Group g4 = new Group(13,"接口管理") ;
//		Group g5 = new Group(14,"备份管理") ;
//		Action ac01 = new Action(1001,"新闻发布","--") ;
//		Action ac02 = new Action(1002,"新闻列表","--") ;
//		Action ac03 = new Action(1003,"新闻审核","--") ;
//		Action ac04 = new Action(1004,"增加用户","--") ;
//		Action ac05 = new Action(1005,"用户列表","--") ;
//		Action ac06 = new Action(1006,"登陆日志","--") ;
//		Action ac07 = new Action(1007,"雇员数据","--") ;
//		Action ac08 = new Action(1008,"部门数据","--") ;
//		Action ac09 = new Action(1009,"公司数据","--") ;
//		Action ac10 = new Action(1010,"服务传输","--") ;
//		Action ac11 = new Action(1011,"短信平台","--") ;
//		Action ac12 = new Action(1012,"全部备份","--") ;
//		Action ac13 = new Action(1013,"局部备份","--") ;
//		//2.设置这些对象之间的基本关系
//		//设置管理员与角色
//		a1.setRole(r1) ;
//		a2.setRole(r2) ;
//		a3.setRole(r2) ;
//		r1.setAdmins(new Admin[] {a1}) ;
//		r2.setAdmins(new Admin[] {a2,a3}) ;
//		//设置角色与管理员组
//		r1.setGroups(new Group[] {g1,g2,g3,g4,g5}) ;
//		r2.setGroups(new Group[] {g1,g2}) ;
//		g1.setRoles(new Role[] {r1,r2}) ;
//		g2.setRoles(new Role[] {r1,r2}) ;
//		g3.setRoles(new Role[] {r1}) ;
//		g4.setRoles(new Role[] {r1}) ;
//		g5.setRoles(new Role[] {r1}) ;
//		//设置管理员组与权限	
//		g1.setActions(new Action[]{ac01,ac02,ac03}) ;
//		g2.setActions(new Action[]{ac04,ac05,ac06}) ;
//		g3.setActions(new Action[]{ac07,ac08,ac09}) ;
//		g4.setActions(new Action[]{ac10,ac11}) ;
//		g5.setActions(new Action[]{ac12,ac13}) ;
//		ac01.setGroup(g1) ;
//		ac02.setGroup(g1) ;
//		ac03.setGroup(g1) ;
//		ac04.setGroup(g2) ;
//		ac05.setGroup(g2) ;
//		ac06.setGroup(g2) ;
//		ac07.setGroup(g3) ;
//		ac08.setGroup(g3) ;
//		ac09.setGroup(g3) ;
//		ac10.setGroup(g4) ;
//		ac11.setGroup(g4) ;
//		ac12.setGroup(g5) ;
//		ac13.setGroup(g5) ;
//		//第二部：取出数据内容
//		System.out.println(a1.getInfo()) ;
//		System.out.println("\t|- " + a1.getRole().getInfo()) ;
//		for(int x = 0 ; x < a1.getRole().getGroups().length ; x ++) {
//			System.out.println("\t\t|-" + a1.getRole().getGroups()[x].getInfo()) ;
//			for(int y = 0; y < a1.getRole().getGroups()[x].getActions().length ; y ++) {
//				System.out.println("\t\t\t|-- " + a1.getRole().getGroups()[x].getActions()[y].getInfo) ; 
//			}
//		}
//		System.out.println("--------------------------------------") ;
//		System.out.println(g2.getInfo()) ;
//		
//	}
//}