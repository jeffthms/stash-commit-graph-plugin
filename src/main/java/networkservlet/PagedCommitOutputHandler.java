package networkservlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.io.LineReader;
import com.atlassian.bitbucket.io.LineReaderOutputHandler;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scm.CommandOutputHandler;
import com.atlassian.bitbucket.scm.CommitsCommandParameters;
import com.atlassian.bitbucket.scm.git.command.CommitReader;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequest;
import com.atlassian.bitbucket.util.PageUtils;

public class PagedCommitOutputHandler extends LineReaderOutputHandler implements CommandOutputHandler<Page<Commit>>
{
	private final List<Commit> commits;
	private final PageRequest pageRequest;
	private final CommitReader commitReader;

	public PagedCommitOutputHandler( Repository repository, CommitsCommandParameters parameters, PageRequest pageRequest )
	{
		super( "UTF-8" );
		this.commitReader = new CommitReader( repository, parameters.isWithMessages() )
		{
			protected void ping()
			{
				PagedCommitOutputHandler.this.resetWatchdog();
			}
		};

		this.pageRequest = pageRequest;
		this.commits = new ArrayList<>( pageRequest.getLimit() + 1 );
	}

	public Page<Commit> getOutput()
	{
		return PageUtils.createPage( this.commits, this.pageRequest );
	}

	protected Commit readCommit( LineReader reader ) throws IOException
	{
		return this.commitReader.readCommit( reader );
	}

	protected void processReader( LineReader reader ) throws IOException
	{
		int count = 0;

		while ( true )
		{
			Commit commit = readCommit( reader );
			if ( commit == null )
			{
				break;
			}

			if ( count++ >= this.pageRequest.getStart() )
			{
				this.commits.add( commit );
				if ( this.commits.size() > this.pageRequest.getLimit() )
				{
					this.cancelProcess();
					break;
				}
			}
		}

	}
}
