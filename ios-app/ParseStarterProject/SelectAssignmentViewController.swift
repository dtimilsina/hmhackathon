//
//  SelectAssignmentViewController.swift
//  ParseStarterProject
//
//  Created by Diwas  Timilsina on 6/27/15.
//  Copyright (c) 2015 Parse. All rights reserved.
//

import UIKit

class SelectAssignmentViewController: UIViewController {

    @IBOutlet weak var selectAssignmentType: UISegmentedControl!
    
    override func viewDidLoad() {
        super.viewDidLoad()
        
 
        //selectAssignmentType.addTarget(self, action:"assignmentChanged:" , forControlEvents: .ValueChanged)
    }

    override func didReceiveMemoryWarning() {
        super.didReceiveMemoryWarning()
        // Dispose of any resources that can be recreated.
    }
    
    
    @IBAction func actionButtonTap(sender: AnyObject) {
        if selectAssignmentType.selectedSegmentIndex == 1 {
            self.performSegueWithIdentifier("checkAssignmentSegue", sender: self)
        }else {
            self.performSegueWithIdentifier("createAssignmentSegue", sender: self)
        }
    }
    
    // MARK: - Navigation

    // In a storyboard-based application, you will often want to do a little preparation before navigation
    override func prepareForSegue(segue: UIStoryboardSegue, sender: AnyObject?) {
        // Get the new view controller using segue.destinationViewController.
        // Pass the selected object to the new view controller.
        
        if segue.identifier == "checkAssignmentSegue" {
            let assignmentVC = segue.destinationViewController as! CheckingViewController
            
        }
        
        if segue.identifier == "createAssignmentSegue" {
            let assignmentVC = segue.destinationViewController as! CreatingTableViewController
            
        }
    }


}
