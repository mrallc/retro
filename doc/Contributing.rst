============
Contributing
============

Starting with the 11.3 release, we are using the services of Launchpad to
manage the repositories and bug tracking. While not mandatory, it is
recommended that you create a Launchpad account if you plan to contribute
to the main repository.


-----------
Bug Reports
-----------

Bugs can be reported to the mailing list or via the bug tracker on
Launchpad. I would prefer use of the bug tracker.


------
Bazaar
------

With 11.3, we have moved development to a Bazaar repository. This should
make handling incoming patchs somewhat easier. You can make a branch, and
we can merge it into the main repository as necessary.

* Create a Launchpad account
* Register an SSH key

Then:

::

  bzr branch lp:retro-language retro-language

In your branch, make changes and commit:

::

  bzr add <files>
  bzr status
  bzr commit -m "commit message"

I recommend using **bzr status** to check exactly which files are being
committed.

And push to a new branch:

::

  bzr push lp:~<username>/retro-language/<branchname>

Replace <username> and <branchname> with your Launchpad account name and
the name of your branch. Once this is done, future **bzr push** invokations
should use the specified branch by default.

When you're ready for me to merge, file a merge request on Launchpad, or
send me an email.


-------
Patches
-------

If you can not use Bazaar and Launchpad, you can email patches (or modified
files and instructions) to crc@retroforth.org. Note that patches will take
longer to merge into the main repository.


-----
Legal
-----

Retro is distributed under the ISC license. This is provided in the LICENSE
file. Unless otherwise noted, any contributions are added under this license.
If this is not acceptable, please advise with any emails or merge requests.
I am willing to accept code under other permissive licenses (e.g., the modern
BSD licenses, MIT/X11 license), but will not include code that is GPL'd or
LGPL'd.

If contributing, please add your name to the end of the LICENSE file, along with
the current year(s). It's also recommended that you add your name and copyright
year(s) to any modified source files.

