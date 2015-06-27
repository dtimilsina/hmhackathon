
//
//  ViewController.swift
//
//  Copyright 2011-present Parse Inc. All rights reserved.
//

import UIKit
import Parse
import ParseUI

class HomeViewController: UIViewController, PFLogInViewControllerDelegate, PFSignUpViewControllerDelegate,UITableViewDataSource, UITableViewDelegate {
    
    @IBOutlet weak var userName: UILabel!
    @IBOutlet weak var logoutButton: UIButton!
    
    @IBOutlet weak var tableView: UITableView!
    var lessonList = ["Kitchen", "Travel", "City", "Family"]
    
    override func viewDidLoad() {
        super.viewDidLoad()
        //self.tableView.registerClass(UITableViewCell.self, forCellReuseIdentifier: "lessonTitle")
        tableView.delegate = self
        tableView.dataSource = self
     
    }

    func presentLoginViewController() {
        let loginViewController = PFLogInViewController()
        loginViewController.delegate = self
        
        var logInLogoTitle = UILabel()
        logInLogoTitle.text = "Remote Translator"
        logInLogoTitle.font = UIFont(name: "HelveticaNeue-Bold", size: 27.5)
        //logInLogoTitle.textColor = UIColor(netHex: 0xF9FFFC)
        loginViewController.logInView!.logo = logInLogoTitle
        
        let signUpViewController = PFSignUpViewController()
        signUpViewController.delegate = self
        
        var SignUpLogoTitle = UILabel()
        SignUpLogoTitle.text = "Admin SignUp"
        SignUpLogoTitle.font = UIFont(name: "HelveticaNeue-Bold", size: 27.5)
        //SignUpLogoTitle.textColor = UIColor(netHex: 0xF9FFFC)
        signUpViewController.signUpView!.logo = SignUpLogoTitle
        
        
        loginViewController.signUpController = signUpViewController
        presentViewController(loginViewController, animated: true, completion: nil)
    }
    
    
    override func viewDidAppear(animated: Bool) {
        super.viewDidAppear(animated)
        
        if PFUser.currentUser() == nil {
            presentLoginViewController()
            logoutButton.enabled = false
        }
        else {
            logoutButton.enabled = true
        }
    }
    
    
    // MARK: - Table view data source
    
    func numberOfSectionsInTableView(tableView: UITableView) -> Int {
        // #warning Potentially incomplete method implementation.
        // Return the number of sections.
        return 1
    }
    
    func tableView(tableView: UITableView, numberOfRowsInSection section: Int) -> Int {
        // #warning Incomplete method implementation.
        // Return the number of rows in the section.
        return lessonList.count
    }
    
  
    func tableView(tableView: UITableView, cellForRowAtIndexPath indexPath: NSIndexPath) -> UITableViewCell {
        let cell = tableView.dequeueReusableCellWithIdentifier("lessonTitle", forIndexPath: indexPath) as! UITableViewCell
        cell.textLabel?.text = lessonList[indexPath.row]
        
        return cell
    }
    

    
    
    //MARK: Parse login
    func logInViewController(logInController: PFLogInViewController, shouldBeginLogInWithUsername username: String, password: String) -> Bool {
        if (!username.isEmpty || !password.isEmpty) {
            return true
        } else {
            return false
        }
    }
    
    func logInViewController(logInController: PFLogInViewController, didLogInUser user: PFUser) {
        dismissViewControllerAnimated(true, completion: nil)
        logoutButton.enabled = true
    }
    
    func logInViewController(logInController: PFLogInViewController, didFailToLogInWithError error: NSError?) {
        
        let alert = UIAlertController(title: "Login Error", message: error?.localizedDescription ?? "Unspecified error", preferredStyle: .Alert)
        
        let okAction = UIAlertAction(title: "Ok", style: .Default, handler: nil)
        alert.addAction(okAction)
        presentViewController(alert, animated: true, completion: nil)
    }
    
    // MARK: Parse signup
    
    func signUpViewController(signUpController: PFSignUpViewController, shouldBeginSignUp info: [NSObject : AnyObject]) -> Bool {
        return true
    }
    
    func signUpViewController(signUpController: PFSignUpViewController, didSignUpUser user: PFUser) {
        self.dismissViewControllerAnimated(true, completion: nil)
        logoutButton.enabled = true
    }
    
    func signUpViewController(signUpController: PFSignUpViewController, didFailToSignUpWithError error: NSError?) {
        let alert = UIAlertController(title: "Signup Error", message: error?.localizedDescription ?? "Unspecified error", preferredStyle: .Alert)
        
        let okAction = UIAlertAction(title: "Ok", style: .Default, handler: nil)
        alert.addAction(okAction)
        presentViewController(alert, animated: true, completion: nil)
    }
    
    func signUpViewControllerDidCancelSignUp(signUpController: PFSignUpViewController) {
        let alert = UIAlertController(title: "Signup Aborted", message: "Are you sure you want to leave!", preferredStyle: .Alert)
        
        let okAction = UIAlertAction(title: "Ok", style: .Default, handler: nil)
        alert.addAction(okAction)
        presentViewController(alert, animated: true, completion: nil)
    }


    
    @IBAction func logoutButtonTap(sender: AnyObject) {
        PFUser.logOut()
        presentLoginViewController()
        logoutButton.enabled = false
    }
    
}

