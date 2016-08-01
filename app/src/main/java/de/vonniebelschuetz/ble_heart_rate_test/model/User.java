package de.vonniebelschuetz.ble_heart_rate_test.model;

import java.io.Serializable;

/**
 * Created by Lincoln on 07/01/16.
 */
public class User implements Serializable {
    String id, name, email, gender;
    String age;

    public User() {
    }
    public User(String id, String name, String email) {
        this.id = id;
        this.name = name;
        this.email = email;
    }
   // public User(String id, String name, String email,String gender, String age) {
     //   this.id = id;
     //   this.name = name;
     //   this.email = email;
     //   this.gender = gender;
     //   this.age = age;
    //}




    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

   // public String getGender() {
     //   return gender;
    //}

    //public void setGender(String gender) {
      //  this.gender = gender;
    //}

//    public String getAge() {
    //    return age;
  //  }
//
    //public void setAge(String age) {
      //  this.age = age;
    //}
}
