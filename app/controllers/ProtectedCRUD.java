package controllers;

import play.mvc.With;

@Check("isAdmin")
@With(RequiresLogin.class)
public class ProtectedCRUD extends CRUD {
}
